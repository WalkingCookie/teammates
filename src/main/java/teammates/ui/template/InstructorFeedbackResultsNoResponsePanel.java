package teammates.ui.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import teammates.common.datatransfer.FeedbackSessionResponseStatus;
import teammates.common.util.Assumption;
import teammates.common.util.Const;

public class InstructorFeedbackResultsNoResponsePanel {
    private List<String> emails;
    private Map<String, String> names;
    private Map<String, String> teams;
    private Map<String, InstructorFeedbackResultsModerationButton> moderationButtons;
    
    public InstructorFeedbackResultsNoResponsePanel(FeedbackSessionResponseStatus responseStatus,
                                    Map<String, InstructorFeedbackResultsModerationButton> moderationButtons) {
        this.names = Collections.unmodifiableMap(responseStatus.emailNameTable);
        this.emails = getFilteredEmails(responseStatus.getStudentsWhoDidNotRespondToAnyQuestion());
        this.teams = getTeamsWithInstructorTeam(responseStatus.emailTeamNameTable,
                                                "<i>" + Const.USER_TEAM_FOR_INSTRUCTOR + "</i>");
        this.moderationButtons = moderationButtons;
    }

    public List<String> getEmails() {
        return emails;
    }

    public Map<String, String> getNames() {
        return names;
    }

    public Map<String, String> getTeams() {
        return teams;
    }

    private List<String> getFilteredEmails(List<String> allEmails) {
        Assumption.assertNotNull(allEmails);
        Assumption.assertNotNull(names);
        
        List<String> emails = new ArrayList<>();
        emails.addAll(allEmails);
        emails.retainAll(names.keySet());
        return emails;
    }
    
    private Map<String, String> getTeamsWithInstructorTeam(Map<String, String> studentTeams,
                                                           String instructorTeamName) {
        Assumption.assertNotNull(emails);
        Assumption.assertNotNull(studentTeams);
        
        Map<String, String> teams = new HashMap<>();
        teams.putAll(studentTeams);
        
        // TODO: Support for users who are both instructor and student
        List<String> instructorEmails = new ArrayList<>();
        instructorEmails.addAll(emails);
        instructorEmails.removeAll(studentTeams.keySet());
        
        for (String email : instructorEmails) {
            teams.put(email, instructorTeamName);
        }
        
        return Collections.unmodifiableMap(teams);
    }
    
    public Map<String, InstructorFeedbackResultsModerationButton> getModerationButtons() {
        return moderationButtons;
    }

}
