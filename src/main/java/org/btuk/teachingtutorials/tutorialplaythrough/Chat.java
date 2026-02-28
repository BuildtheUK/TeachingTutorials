package org.btuk.teachingtutorials.tutorialplaythrough;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.btuk.teachingtutorials.TeachingTutorials;
import org.btuk.teachingtutorials.guis.locationcreatemenus.ChatLocationTaskEditorMenu;
import org.btuk.teachingtutorials.tutorialobjects.Task;
import org.btuk.teachingtutorials.tutorialobjects.LocationTask;
import org.btuk.teachingtutorials.utils.Display;

import java.util.logging.Level;

/**
 * Represents a type of Task where the user must enter a message into chat. Contains the relevant listeners used when the task is active.
 */
public class Chat extends PlaythroughTask implements Listener
{
    /** Stores the target answer(s) or the numerical bounds. Always supports Regex forms when used in discrete mode */
    private String[] szTargetAnswers;

    /** Stores the score to be received upon completing the task using the corresponding indexed answer specified in szTargetAnswers.
     * If using in discrete mode */
    private float[] fScores;

    /** Stores the type of chat that this is */
    private ChatType chatType;

//    Next task is to sort out the setting of answers, also checking thaat : does not appear in any of the asnwers, or come up with a way to solve this

    /**
     * Used when initialising a task for a lesson, i.e when the answers are already known
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for which the task is for
     * @param locationTask A reference to the location task object of this chat
     * @param groupPlaythrough A reference to the parent group play-through object which this task is a member of
     */
    Chat(TeachingTutorials plugin, Player player, LocationTask locationTask, GroupPlaythrough groupPlaythrough)
    {
        super(plugin, player, locationTask, groupPlaythrough);

        //Check which sort of answer(s) is (are) specified and perform the necessary information extraction
        String szAnswerText = locationTask.getAnswer();
        String[] szAnswerParts = szAnswerText.split(":");

        //Handles original mechanics
        if (szAnswerParts.length == 1)
        {
            szTargetAnswers = szAnswerParts;
            fScores = new float[]{1};
            chatType = ChatType.Discrete;
        }
        switch (szAnswerParts[0])
        {
            //Deals with discrete task types
            case "Discrete":
                //Set the chat type
                chatType = ChatType.Discrete;

                //Creates a list of all of the valid answers and their scores. Each entry in this string should have form: "Answer,score"
                String[] szAnswers = szAnswerParts[1].split(";");

                //Initialises the answer and score array to the required length
                int iNumAnswers = szAnswers.length;
                this.szTargetAnswers = new String[iNumAnswers];
                this.fScores = new float[iNumAnswers];

                for (int i = 0 ; i < iNumAnswers ; i++)
                {
                    //Extracts the ith answer and score
                    String szAnswerAndScore = szAnswers[i];

                    //Extracts the answer and the score into different variables, and adds them to the chat task list
                    String szAnswer = szAnswerAndScore.substring(0, szAnswerAndScore.indexOf(','));
                    float fScore = Float.parseFloat(szAnswerAndScore.substring(szAnswerAndScore.indexOf(',')+1));
                    szTargetAnswers[i] = szAnswer;
                    fScores[i] = fScore;
                }

                this.taskEditorMenu = new ChatLocationTaskEditorMenu(plugin,
                        groupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCreatorOrStudent(),
                        groupPlaythrough.getParentStep().getEditorMenu(),
                        Display.colouredText("Chat Task Editor", NamedTextColor.AQUA),
                        this.getLocationTask(), this, szTargetAnswers, fScores);

                break;

            //Deals with numerical task types
            case "Numerical":
                //Set the chat type
                chatType = ChatType.Numerical;

                //Extracts the min, ideal and max scores. This will produce a string array looking like: [min,ideal,max]
                String[] szBounds = szAnswerParts[1].split(",");
                szTargetAnswers = szBounds;

                this.taskEditorMenu = new ChatLocationTaskEditorMenu(plugin,
                        groupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCreatorOrStudent(),
                        groupPlaythrough.getParentStep().getEditorMenu(),
                        Display.colouredText("Chat Task Editor", NamedTextColor.AQUA),
                        this.getLocationTask(), this, szBounds);
                break;
        }
    }

    /**
     * Used when initialising a task when creating a new location
     * @param plugin A reference to the TeachingTutorials plugin instance
     * @param player A reference to the Bukkit player for which the task is for
     * @param task A reference to the task
     * @param groupPlaythrough A reference to the parent group play-through object which this task is a member of
     */
    Chat(TeachingTutorials plugin, Player player, Task task, GroupPlaythrough groupPlaythrough)
    {
        super(plugin, player, task, groupPlaythrough);

        this.taskEditorMenu = new ChatLocationTaskEditorMenu(plugin,
                groupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCreatorOrStudent(),
                groupPlaythrough.getParentStep().getEditorMenu(),
                Display.colouredText("Chat Task Editor", NamedTextColor.AQUA),
                this.getLocationTask(), this);
    }

    /**
     * Registers the task listener, activating the task
     */
    @Override
    public void register()
    {
        PlaythroughMode currentMode = this.parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCurrentPlaythroughMode();

        //Log registration and notify player that they need to use the gui if editing answers
        String szLogMessage = "";
        switch (currentMode)
        {
            case PlayingLesson:
                if (this.parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough() instanceof Lesson lesson)
                    szLogMessage = "Lesson: " +lesson.getLessonID()
                            +". Chat Task: " +this.getLocationTask().iTaskID;
                break;
            case EditingLocation:
                if (this.parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough() instanceof Lesson lesson)
                    szLogMessage = "Lesson: " +lesson.getLessonID()
                            +". Editing Chat Task: " +this.getLocationTask().iTaskID;

                Display.ActionBar(player, Display.colouredText("Use the menu to edit the answers", NamedTextColor.GREEN));
                break;
            case CreatingLocation:
                szLogMessage = "New Location being made by :"+player.getName() +". Chat Task: " +this.getLocationTask().iTaskID;
                Display.ActionBar(player, Display.colouredText("Use the menu to edit the answers", NamedTextColor.GREEN));
                break;
        }

        //Output the answers to console
        if (!currentMode.equals(PlaythroughMode.CreatingLocation))
        {
            if (chatType.equals(ChatType.Numerical))
                szLogMessage = szLogMessage+". Numerical Chat with max: "+szTargetAnswers[0] +", target: "+szTargetAnswers[1] +", and max: "+szTargetAnswers[2];
            else
            {
                szLogMessage = szLogMessage+". Discrete Chat with answers: ";
                int iNumAnswers = szTargetAnswers.length;
                for (int i = 0 ; i < iNumAnswers - 1 ; i++)
                {
                    szLogMessage = szLogMessage + szTargetAnswers[i] +", ";
                }
                szLogMessage = szLogMessage + szTargetAnswers[iNumAnswers-1] +".";
            }
        }

        //Register it
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.getLogger().log(Level.INFO, szLogMessage);
        super.register();
    }

    /**
     * Detects player chat events, determines if it is from the player of this task, and if so performs necessary logic
     * @param event A reference to a player chat event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void chatEvent(AsyncChatEvent event)
    {
        fPerformance = 0F;

        //Checks that this message is from the relevant player, if not, ignores the event
        if (!event.getPlayer().getUniqueId().equals(player.getUniqueId()))
        {
            return;
        }

        //Extracts the message that the user has sent
        String szChat = ((TextComponent) event.message()).content();

        //Performs logic for if it is a lesson
        if (parentGroupPlaythrough.getParentStep().getParentStage().getTutorialPlaythrough().getCurrentPlaythroughMode().equals(PlaythroughMode.PlayingLesson))
        {
            //Hides answers
            event.setCancelled(true);

            if (chatType.equals(ChatType.Numerical))
            {
                try
                {
                    float fPlayerAnswer = Float.parseFloat(szChat);
                    float fMin = Float.parseFloat(szTargetAnswers[0]);
                    float fIdeal = Float.parseFloat(szTargetAnswers[1]);
                    float fMax = Float.parseFloat(szTargetAnswers[2]);

                    if (fPlayerAnswer >= fMin && fPlayerAnswer <= fMax)
                    {
                        //Answer correct
                        Display.ActionBar(player, Display.colouredText("Correct answer !", NamedTextColor.DARK_GREEN));
                        messageCorrect();

                        //Calculates the score
                        if (fPlayerAnswer == fIdeal)
                            fPerformance = 1F;
                        else
                        {
                            float m, c;
                            float fBound;
//                            if (fPlayerAnswer < fIdeal)
//                            {
//                                m = 1.0f/(fIdeal-fMin);
//                                c = -fMin*m;
//                            }
//                            else
//                            {
//                                m = -1.0f/(fMax-fIdeal);
//                                c = -fMax*m;
//                            }

                            //Simplifies to:
                            if (fPlayerAnswer < fIdeal)
                                fBound = fMin;
                            else
                                fBound = fMax;

//                          Non-zero fIdeal-fBound situation would never occur:
                            //Answer is in the bounds by this point
                            //But isn't equal to the ideal
                            //If Min and Max were both the same as ideal then to be in the bounds it would have to be ideal so this isn't reached
                            //If Min is the same as ideal then to be in the bounds it must be:
                            // Less than the ideal: it can't be since that would be less than the min
                            // Greater than the ideal, in which case upper bound would take over which isn't same as ideal
                            //If Max is the same as ideal then to be in the bounds it must be:
                            // Less than the ideal, in which case the lower bound would take over which isn't same as ideal
                            // Greater than the ideal, it can't be since that would be greater than the max

                            //For safety:

                            //Handles non zero error. Note I can't see when this would ever happen but is here in case there is a way
                            if (fIdeal-fBound == 0f)
                                fPerformance = 1;
                            else
                            {
                                m = 1.0f/(fIdeal-fBound);
                                c = -fBound*m;
                                fPerformance = m*fPlayerAnswer+c;
                            }
                        }
                        return;
                    }

                }
                catch (NumberFormatException e)
                {
                    //Prompt
                    Display.ActionBar(player, Display.colouredText("The answer should be a number, try again", NamedTextColor.GOLD));
                    return;
                }
            }

            //Logic for discrete answers
            else
            {
                int iNumAnswers = szTargetAnswers.length;
                for (int i = 0 ; i < iNumAnswers ; i++)
                {
                    if (szChat.toLowerCase().matches(szTargetAnswers[i].toLowerCase()))
                    {
                        //Answer correct
                        Display.ActionBar(player, Display.colouredText("Correct answer !", NamedTextColor.DARK_GREEN));
                        messageCorrect();
                        fPerformance = fScores[i];
                        return;
                    }
                }
            }

            //If not found a correct answer:

            //Answer incorrect
            Display.ActionBar(player, Display.colouredText("Incorrect, try again", NamedTextColor.GOLD));
        }

    }

    /**
     * Handles completion of the task - unregisters listeners and moves onto the next task
     */
    private void messageCorrect()
    {
        //Unregisters this task
        HandlerList.unregisterAll(this);

        //Marks the task as complete, move on to next task
        taskComplete();
    }

    /**
     * Unregisters the listener, marks the task as inactive and removes the virtual blocks of this task
     */
    public void unregister()
    {
        //Marks the task as inactive and removes the virtual blocks of this task
        super.deactivate();

        //Unregisters this task
        HandlerList.unregisterAll(this);
    }

    //A public version is required for when spotHit is called from the difficulty panel
    //This is required as it means that the tutorial can be halted until the difficulty panel completes the creation of the new LocationTask
    /**
     * To be called from a difficulty panel when the difficulty has been specified.
     * <p> </p>
     * Will unregister the chat task and move forwards to the next task
     */
    public void newLocationSpotHit()
    {
        messageCorrect();
    }
}

enum ChatType
{
    Numerical, Discrete
}
