package teachingtutorials.newtutorial;

import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.adminscreators.CreatorTutorialsMenu;
import teachingtutorials.guis.TutorialGUIUtils;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;

/**
 * Represents and handles a tutorial creation session
 */
public class TutorialCreationSession
{
    /** A reference to an instance of the TeachingTutorials plugin */
    final TeachingTutorials plugin;

    /** A reference to the creator of this new tutorial */
    final User creator;

    /** A NewTutorial object to hold the details of the new tutorial */
    final Tutorial newTutorial;

    /** The master menu for the new tutorial creation */
    private NewTutorialMenu menu;

    /**
     * Constructs a new Tutorial creation session
     * @param plugin A reference to an instance of the TeachingTutorials plugin
     * @param creator A reference to the creator of this new tutorial
     */
    public TutorialCreationSession(TeachingTutorials plugin, User creator)
    {
        this.plugin = plugin;
        this.creator = creator;

        //Creates the new tutorial
        newTutorial = new Tutorial("", creator.player.getUniqueId().toString());
    }

    /**
     * Launches the tutorial creation session
     */
    public void startSession()
    {
        //Set up menu
        menu = new NewTutorialMenu(this, newTutorial);
        creator.mainGui = menu;
        //Open the menu for the creator
        menu.open(creator.player);


        //Any further logic is handled by the menus themselves
    }

    /**
     * Terminates the session without adding the tutorial
     */
    void terminateSession()
    {
        //Close inventories and delete guis
        creator.player.closeInventory();

        //Take player to creator tutorials menu
        CreatorTutorialsMenu creatorTutorialsMenu = new CreatorTutorialsMenu(plugin, creator, Tutorial.fetchAll(false, false, creator.player.getUniqueId(), plugin.getDBConnection(), plugin.getLogger()));
        creator.mainGui = creatorTutorialsMenu;
        creator.mainGui.open(creator.player);

        //Delete the new tutorial menu
        menu.delete();
    }

    /**
     * Checks whether tutorial is ready.
     * <p>If ready, ask the user to confirm via a menu</p>
     * <p>If not ready, output to user</p>
     */
    void attemptSave()
    {
        //Audit the tutorial information to ensure completion
        if (!newTutorial.isComplete())
        {
            if (newTutorial.getTutorialName().equals(""))
                creator.player.sendMessage(Display.errorText("Tutorials is not ready to add. Make sure to give the tutorial a name."));
            else
                creator.player.sendMessage(Display.errorText("Tutorial is not ready to add. The unenchanted book will lead you to the unready component"));
        }
        else
        {
            //Open up a verify menu
            ConfirmConfirmation confirmConfirmation = new ConfirmConfirmation(menu, new Runnable() {
                @Override
                public void run() {
                    creator.player.closeInventory();

                    addTutorial();
                }
            }, TutorialGUIUtils.optionLore("Add tutorial to the database"), TutorialGUIUtils.optionLore("Back to tutorial menu"));
            confirmConfirmation.open(creator.player);
        }
    }

    /**
     * Adds the new tutorial to the database then opens the creator tutorials menu
     */
    private void addTutorial()
    {
        //Add the new tutorial to the database
        newTutorial.addTutorialToDB(plugin.getDBConnection(), plugin.getLogger());

        //Take player to creator tutorials menu
        CreatorTutorialsMenu creatorTutorialsMenu = new CreatorTutorialsMenu(plugin, creator, Tutorial.fetchAll(false, false, creator.player.getUniqueId(), plugin.getDBConnection(), plugin.getLogger()));
        creator.mainGui = creatorTutorialsMenu;
        creator.mainGui.open(creator.player);

        menu.delete();
    }
}
