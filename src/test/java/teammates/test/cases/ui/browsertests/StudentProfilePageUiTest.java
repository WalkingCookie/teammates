package teammates.test.cases.ui.browsertests;

import org.openqa.selenium.By;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.StudentProfileAttributes;
import teammates.common.util.AppUrl;
import teammates.common.util.Const;
import teammates.common.util.StringHelper;
import teammates.logic.backdoor.BackDoorServlet;
import teammates.test.driver.BackDoor;
import teammates.test.driver.TestProperties;
import teammates.test.pageobjects.AppPage;
import teammates.test.pageobjects.Browser;
import teammates.test.pageobjects.BrowserPool;
import teammates.test.pageobjects.EntityNotFoundPage;
import teammates.test.pageobjects.GenericAppPage;
import teammates.test.pageobjects.NotAuthorizedPage;
import teammates.test.pageobjects.NotFoundPage;
import teammates.test.pageobjects.StudentHomePage;
import teammates.test.pageobjects.StudentProfilePage;
import teammates.test.pageobjects.StudentProfilePicturePage;
import teammates.test.util.Priority;

@Priority(-3)
public class StudentProfilePageUiTest extends BaseUiTestCase {
    private static Browser browser;
    private static DataBundle testData;
    private StudentProfilePage profilePage;

    @BeforeClass
    public static void classSetup() {
        printTestClassHeader();
        testData = loadDataBundle("/StudentProfilePageUiTest.json");
        
        // use the 2nd student account injected for this test
        
        String student2GoogleId = TestProperties.TEST_STUDENT2_ACCOUNT;
        String student2Email = student2GoogleId + "@gmail.com";
        testData.accounts.get("studentWithExistingProfile").googleId = student2GoogleId;
        testData.accounts.get("studentWithExistingProfile").email = student2Email;
        testData.accounts.get("studentWithExistingProfile").studentProfile.googleId = student2GoogleId;
        testData.students.get("studentWithExistingProfile").googleId = student2GoogleId;
        testData.students.get("studentWithExistingProfile").email = student2Email;
        
        removeAndRestoreTestDataOnServer(testData);
        browser = BrowserPool.getBrowser();
    }

    @Test
    public void allTests() throws Exception {
        // Do not change the order
        testNavLinkToPage();
        testContent();
        testActions();
        testJsFunctions();
        testAjaxPictureUrl();
    }

    private void testJsFunctions() {
        ______TS("Test disabling and enabling of upload button");
        // initial disabled state
        profilePage.verifyUploadButtonState(false);
        
        //enabled when a file is selected
        profilePage.fillProfilePic("src/test/resources/images/profile_pic.png");
        profilePage.verifyUploadButtonState(true);
        
        // disabled when file is cancelled
        profilePage.fillProfilePic("");
        profilePage.verifyUploadButtonState(false);
        
        // re-enabled when a new file is selected
        profilePage.fillProfilePic("src/test/resources/images/profile_pic.png");
        profilePage.verifyUploadButtonState(true);
        
    }

    private void testNavLinkToPage() {
        AppUrl profileUrl = createUrl(Const.ActionURIs.STUDENT_HOME_PAGE)
                                   .withUserId(testData.accounts.get("studentWithEmptyProfile").googleId);
        StudentHomePage shp = loginAdminToPage(browser, profileUrl, StudentHomePage.class);
        profilePage = shp.loadProfileTab().changePageType(StudentProfilePage.class);
    }

    private void testContent() throws Exception {
        // assumes it is run after NavLinks Test
        // (ie already logged in as studentWithEmptyProfile
        ______TS("Typical case: empty profile values");

        // This is the full HTML verification for Registered Student Profile Submit Page, the rest can all be verifyMainHtml
        profilePage.verifyHtml("/studentProfilePageDefault.html");

        ______TS("Typical case: existing profile values");
        // this test uses actual user accounts
        profilePage = getProfilePageForStudent("studentWithExistingProfile");
        profilePage.verifyHtmlPart(By.id("editProfileDiv"), "/studentProfileEditDivExistingValues.html");

        ______TS("Typical case: edit profile picture modal (without existing picture)");
        profilePage = getProfilePageForStudent("studentWithExistingProfile");
        profilePage.showPictureEditor();
        profilePage.verifyHtmlPart(By.id("studentPhotoUploader"), "/studentProfilePictureModalDefault.html");

        ______TS("Typical case: edit profile picture modal (with existing picture)");
        profilePage = getProfilePageForStudent("studentWithExistingProfile");
        profilePage.fillProfilePic("src/test/resources/images/profile_pic.png");
        profilePage.uploadPicture();

        profilePage.verifyStatus(Const.StatusMessages.STUDENT_PROFILE_PICTURE_SAVED);
        profilePage.waitForUploadEditModalVisible();
        profilePage.verifyHtmlMainContent("/studentProfilePageFilled.html");
        
        profilePage.closeEditPictureModal();
    }

    private void testActions() {
        // assumes it is run after NavLinks Test
        // (ie already logged in as studentWithExistingProfile
        String studentGoogleId = testData.accounts.get("studentWithExistingProfile").googleId;

        ______TS("Typical case: no picture");

        profilePage.editProfileThroughUi("", "short.name", "e@email.tmt", "inst", "Usual Nationality",
                                         "male", "this is enough!$%&*</>");
        profilePage.ensureProfileContains("short.name", "e@email.tmt", "inst", "Usual Nationality",
                                          "male", "this is enough!$%&*</>");
        profilePage.verifyStatus(Const.StatusMessages.STUDENT_PROFILE_EDITED);
        
        ______TS("Typical case: changing genders for complete coverage");

        profilePage.editProfileThroughUi("", "short.name", "e@email.tmt", "inst", "Usual Nationality",
                                         "other", "this is enough!$%&*</>");
        profilePage.ensureProfileContains("short.name", "e@email.tmt", "inst", "Usual Nationality",
                                          "other", "this is enough!$%&*</>");
        profilePage.editProfileThroughUi("", "short.name", "e@email.tmt", "inst", "Usual Nationality",
                                        "female", "this is enough!$%&*</>");
        profilePage.ensureProfileContains("short.name", "e@email.tmt", "inst", "Usual Nationality",
                                         "female", "this is enough!$%&*</>");

        ______TS("Failure case: script injection");

        StudentProfileAttributes spa = new StudentProfileAttributes("valid.id",
                                                                    "<script>alert(\"Hello world!\");</script>",
                                                                    "e@email.tmt", " inst", "Usual Nationality",
                                                                    "male", "this is enough!$%&*</>", "");
        profilePage.editProfileThroughUi(spa.googleId, spa.shortName, spa.email, spa.institute, spa.nationality,
                                         spa.gender, spa.moreInfo);
        profilePage.ensureProfileContains("short.name", "e@email.tmt", "inst", "Usual Nationality",
                                          "female", "this is enough!$%&*</>");
        profilePage.verifyStatus(StringHelper.toString(spa.getInvalidityInfo(), " ")
                                             // de-sanitize
                                             .replace("&lt;", "<").replace("&gt;", ">")
                                             .replace("&quot;", "\"").replace("&#x2f;", "/"));
        
        ______TS("Failure case: invalid data");

        spa = new StudentProfileAttributes("valid.id", "$$short.name", "e@email.tmt", " inst  ",
                                           StringHelper.generateStringOfLength(54),
                                           "male", "this is enough!$%&*</>", "");
        profilePage.editProfileThroughUi("", spa.shortName, spa.email, spa.institute, spa.nationality,
                                         spa.gender, spa.moreInfo);
        profilePage.ensureProfileContains("short.name", "e@email.tmt", "inst", "Usual Nationality",
                                          "female", "this is enough!$%&*</>");
        profilePage.verifyStatus(StringHelper.toString(spa.getInvalidityInfo(), " "));

        ______TS("Typical case: picture upload and edit");

        profilePage.fillProfilePic("src/test/resources/images/profile_pic.png");
        profilePage.uploadPicture();

        profilePage.verifyStatus(Const.StatusMessages.STUDENT_PROFILE_PICTURE_SAVED);
        profilePage.waitForUploadEditModalVisible();

        profilePage.editProfilePhoto();
        profilePage.ensureProfileContains("short.name", "e@email.tmt", "inst", "Usual Nationality",
                                          "female", "this is enough!$%&*</>");
        profilePage.verifyPhotoSize(150, 150);

        String prevPictureKey = BackDoor.getStudentProfile(studentGoogleId).pictureKey;
        verifyPictureIsPresent(prevPictureKey);

        ______TS("Typical case: repeated edit");
        
        profilePage.showPictureEditor();
        profilePage.editProfilePhoto();
        profilePage.ensureProfileContains("short.name", "e@email.tmt", "inst", "Usual Nationality",
                                          "female", "this is enough!$%&*</>");
        profilePage.verifyPhotoSize(150, 150);
        
        prevPictureKey = BackDoor.getStudentProfile(studentGoogleId).pictureKey;
        verifyPictureIsPresent(prevPictureKey);
        
        ______TS("Failure case: not a picture");

        profilePage.fillProfilePic("src/test/resources/images/not_a_picture.txt");
        profilePage.uploadPicture();

        profilePage.verifyStatus(Const.StatusMessages.STUDENT_PROFILE_NOT_A_PICTURE);
        verifyPictureIsPresent(prevPictureKey);

        ______TS("Failure case: picture too large");

        profilePage.fillProfilePic("src/test/resources/images/profile_pic_too_large.jpg");
        profilePage.uploadPicture();

        profilePage.verifyStatus(Const.StatusMessages.STUDENT_PROFILE_PIC_TOO_LARGE);
        verifyPictureIsPresent(prevPictureKey);

        ______TS("Typical case: update picture (too tall)");

        profilePage.fillProfilePic("src/test/resources/images/image_tall.jpg");
        profilePage.uploadPicture();
        
        profilePage.verifyStatus(Const.StatusMessages.STUDENT_PROFILE_PICTURE_SAVED);
        profilePage.waitForUploadEditModalVisible();
        profilePage.verifyPhotoSize(3074, 156);

        String currentPictureKey = BackDoor.getStudentProfile(studentGoogleId).pictureKey;
        verifyPictureIsPresent(currentPictureKey);
    }

    private void testAjaxPictureUrl() {
        String studentId = "studentWithExistingProfile";
        String instructorId = "SHomeUiT.instr";
        String helperId = "SHomeUiT.helper";
        String studentGoogleId = testData.accounts.get("studentWithExistingProfile").googleId;
        String currentPictureKey = BackDoor.getStudentProfile(studentGoogleId).pictureKey;
        String email = testData.students.get("studentWithExistingProfile").email;
        String courseId = testData.students.get("studentWithExistingProfile").course;

        email = StringHelper.encrypt(email);
        courseId = StringHelper.encrypt(courseId);
        String invalidEmail = StringHelper.encrypt("random-EmAIl");

        ______TS("Typical case: with blob-key");

        getProfilePicturePage(studentId, currentPictureKey, StudentProfilePicturePage.class).verifyHasPicture();

        ______TS("Failure case: invalid blob-key");

        String invalidKey = "random-StRing123";
        if (TestProperties.isDevServer()) {
            getProfilePicturePage(studentId, invalidKey, NotFoundPage.class);
        } else {
            getProfilePicturePage(studentId, invalidKey, GenericAppPage.class);
            assertEquals("", browser.driver.findElement(By.tagName("body")).getText());
        }

        ______TS("Typical case: with email and course");

        getProfilePicturePage(instructorId, email, courseId, StudentProfilePicturePage.class).verifyHasPicture();

        ______TS("Failure case: instructor does not have privilege");

        getProfilePicturePage(helperId, email, courseId, NotAuthorizedPage.class);

        ______TS("Failure case: non-existent student");

        getProfilePicturePage(instructorId, invalidEmail, courseId, EntityNotFoundPage.class);
    }

    private <T extends AppPage> T getProfilePicturePage(String instructorId, String email, String courseId,
                                                        Class<T> typeOfPage) {
        AppUrl profileUrl = createUrl(Const.ActionURIs.STUDENT_PROFILE_PICTURE)
                                   .withUserId(testData.accounts.get(instructorId).googleId)
                                   .withParam(Const.ParamsNames.STUDENT_EMAIL, email)
                                   .withParam(Const.ParamsNames.COURSE_ID, courseId);
        return loginAdminToPage(browser, profileUrl, typeOfPage);
    }

    private <T extends AppPage> T getProfilePicturePage(String studentId, String pictureKey, Class<T> typeOfPage) {
        AppUrl profileUrl = createUrl(Const.ActionURIs.STUDENT_PROFILE_PICTURE)
                                   .withUserId(testData.accounts.get(studentId).googleId)
                                   .withParam(Const.ParamsNames.BLOB_KEY, pictureKey);
        return loginAdminToPage(browser, profileUrl, typeOfPage);
    }

    private void verifyPictureIsPresent(String pictureKey) {
        assertEquals(BackDoorServlet.RETURN_VALUE_TRUE, BackDoor.getWhetherPictureIsPresentInGcs(pictureKey));
    }

    private StudentProfilePage getProfilePageForStudent(String studentId) {
        AppUrl profileUrl = createUrl(Const.ActionURIs.STUDENT_PROFILE_PAGE)
                                   .withUserId(testData.accounts.get(studentId).googleId);
        return loginAdminToPage(browser, profileUrl, StudentProfilePage.class);
    }

    @AfterClass
    public static void classTearDown() {
        BrowserPool.release(browser);
    }

}
