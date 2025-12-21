package teachingtutorials.guis.locationcreatemenus;

import net.bteuk.minecraft.gui.GuiAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.meta.BookMeta;
import teachingtutorials.TeachingTutorials;
import teachingtutorials.guis.TutorialGUIUtils;
import net.bteuk.minecraft.texteditorbooks.*;
import teachingtutorials.tutorialobjects.LocationTask;
import teachingtutorials.tutorialplaythrough.PlaythroughTask;
import teachingtutorials.utils.Display;
import teachingtutorials.utils.User;
import teachingtutorials.utils.Utils;

import java.util.ArrayList;

/**
 * The version of the LocationTask editor menu which is used to edit the answers of a chat task
 */
public class ChatLocationTaskEditorMenu extends LocationTaskEditorMenu
{
    /** Defines whether the menu is in discrete mode or numerical mode*/
    private Mode mode;

    /** Stores the values for a discrete chat task */
    private ArrayList<TextEditorBookListener> answerBooks = new ArrayList<>();
    private ArrayList<TextEditorBookListener> scoreBooks = new ArrayList<>();

    /** Stores the values for a numerical chat task */
    private TextEditorBookListener numericalMin;
    private TextEditorBookListener numericalPerfect;
    private TextEditorBookListener numericalMax;

    /** The number of pages that the menu has */
    private int iPages;

    /** The current page a player is looking at. Indexed from 1 */
    private int iCurrentPage;

    /**
     * Used when initialising a ChatLocationTaskEditorMenu when the location task in edit mode for a discrete task
     * <p>Will prefill the discrete options with the answers for the task and the numerical options with the defaults</p>
     * @param plugin
     * @param user
     * @param parentStepMenu
     * @param inventoryTitle
     * @param locationTask
     * @param playthroughTask
     * @param answers
     * @param fScores
     */
    public ChatLocationTaskEditorMenu(TeachingTutorials plugin, User user, StepEditorMenu parentStepMenu, Component inventoryTitle, LocationTask locationTask, PlaythroughTask playthroughTask, String[] answers, float[] fScores)
    {
        //Creates the base menu
        super(plugin, user, parentStepMenu, inventoryTitle, locationTask, playthroughTask);

        mode = Mode.Discrete;

        //Numerical setup
        defaultTheNumericalBooks();

        //Add the discrete items
        for (int i = 0 ; i < answers.length ; i++)
        {
            // Create the answer book
            TextEditorBookListener answerBook = new TextEditorBookListener(plugin, user.player, this, ("Answer "+(i+1)),
                    new AnswerBookClose(user, this, false, false),
                    answers[i]);
            answerBooks.add(answerBook);

            // Create the score book
            TextEditorBookListener scoreBook = new TextEditorBookListener(plugin, user.player, this, ("Score "+(i+1)), new BookCloseAction()
            {
                @Override
                public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent)
                {
                    //The false here basically just ensures that even if somehow this listener was used to edit the book despite not being registered, it will never save
                    return false;
                }

                @Override
                public boolean runBookSign(BookMeta bookMeta, BookMeta bookMeta1, TextEditorBookListener textEditorBookListener, String s) {
                    return runBookClose(bookMeta, bookMeta1, textEditorBookListener, s);
                }

                @Override
                public void runPostClose()
                {
                    ChatLocationTaskEditorMenu.this.saveAndCheck();
                    ChatLocationTaskEditorMenu.this.open(user.player);
                }
            }, ""+fScores[i]);
            scoreBooks.add(scoreBook);
        }

        // Set the current page to 1
        iCurrentPage = 1;

        //Add chat specific items
        addAdditionalOptions();
    }

    /**
     * Used when initialising a ChatLocationTaskEditorMenu when the location task in edit mode for a numerical task
     * <p>Will prefill the numerical options with the answers for the task and the discrete options with the defaults</p>
     * @param plugin
     * @param user
     * @param parentStepMenu
     * @param inventoryTitle
     * @param locationTask
     * @param playthroughTask
     * @param bounds
     */
    public ChatLocationTaskEditorMenu(TeachingTutorials plugin, User user, StepEditorMenu parentStepMenu, Component inventoryTitle, LocationTask locationTask, PlaythroughTask playthroughTask, String[] bounds)
    {
        //Creates the base menu
        super(plugin, user, parentStepMenu, inventoryTitle, locationTask, playthroughTask);

        mode = Mode.Numerical;

        //Discrete setup
        defaultTheDiscreteBooks();

        //Numerical setup
        numericalMin = new TextEditorBookListener(plugin, user.player, this, "Minimum", new NumericalBookClose(user, this, Numerical.Min), bounds[0]);
        numericalPerfect = new TextEditorBookListener(plugin, user.player, this, "Perfect", new NumericalBookClose(user, this, Numerical.Target), bounds[1]);
        numericalMax = new TextEditorBookListener(plugin, user.player, this, "Maximum", new NumericalBookClose(user, this, Numerical.Max), bounds[2]);

        // Set the current page to 1
        iCurrentPage = 1;

        //Add chat specific items
        addAdditionalOptions();
    }

    /**
     * Used when initialising a ChatLocationTaskEditorMenu when the location task in location creation mode
     * <p>Will prefill the information with the defaults</p>
     * @param plugin
     * @param user
     * @param parentStepMenu
     * @param inventoryTitle
     * @param locationTask
     * @param playthroughTask
     */
    public ChatLocationTaskEditorMenu(TeachingTutorials plugin, User user, StepEditorMenu parentStepMenu, Component inventoryTitle, LocationTask locationTask, PlaythroughTask playthroughTask)
    {
        //Creates the base menu
        super(plugin, user, parentStepMenu, inventoryTitle, locationTask, playthroughTask);

        //Default to discrete mode
        mode = Mode.Discrete;

        //Discrete setup
        defaultTheDiscreteBooks();

        //Numerical setup
        defaultTheNumericalBooks();

        // Set the current page to 1
        iCurrentPage = 1;

        //Add chat specific items
        addAdditionalOptions();
    }

    private void defaultTheNumericalBooks()
    {
        numericalMin = new TextEditorBookListener(plugin, user.player, this, "Minimum", new NumericalBookClose(user, this, Numerical.Min), "5");
        numericalPerfect = new TextEditorBookListener(plugin, user.player, this, "Perfect", new NumericalBookClose(user, this, Numerical.Target), "10");
        numericalMax = new TextEditorBookListener(plugin, user.player, this, "Maximum", new NumericalBookClose(user, this, Numerical.Max), "15");
    }

    private void defaultTheDiscreteBooks()
    {
        // Create the first discrete item
        TextEditorBookListener firstItem = new TextEditorBookListener(plugin, user.player, this, "Answer 1",
                new AnswerBookClose(user, this, false, false),
                "One of your options must have a performance of 1, this option has been preset to have a performance of 1. Use it as your \"perfect\" answer. \n\nReplace all of this text with your answer - ctrl A, delete");
        answerBooks.add(firstItem);

        // Create the score book
        TextEditorBookListener firstItemScore = new TextEditorBookListener(plugin, user.player, this, "Score 1", new BookCloseAction()
        {
            @Override
            public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent)
            {
                //The false here basically just ensures that even if somehow this listener was used to edit the book despite not being registered, it will never save
                return false;
            }

            @Override
            public boolean runBookSign(BookMeta bookMeta, BookMeta bookMeta1, TextEditorBookListener textEditorBookListener, String s) {
                return runBookClose(bookMeta, bookMeta1, textEditorBookListener, s);
            }

            @Override
            public void runPostClose()
            {
                ChatLocationTaskEditorMenu.this.saveAndCheck();
                ChatLocationTaskEditorMenu.this.open(user.player);
            }
        }, "1.0");
        scoreBooks.add(firstItemScore);
    }

    float getNumericalMin()
    {
        String szText = ((TextComponent) ((BookMeta) numericalMin.getBook().getItemMeta()).pages().get(0)).content();
        return Float.parseFloat(szText);
    }

    float getNumericalTarget()
    {
        String szText = ((TextComponent) ((BookMeta) numericalPerfect.getBook().getItemMeta()).pages().get(0)).content();
        return Float.parseFloat(szText);
    }

    float getNumericalMax()
    {
        String szText = ((TextComponent) ((BookMeta) numericalMax.getBook().getItemMeta()).pages().get(0)).content();
        return Float.parseFloat(szText);
    }

    /**
     * Adds the appropriate chat items to the menu
     */
    @Override
    protected void addAdditionalOptions()
    {
        //Top row will always be the one to switch between discrete and numerical
        //You are in X mode, click to switch to !X mode

        Mode modeNotIn;

        if (mode.equals(Mode.Discrete))
            modeNotIn = Mode.Numerical;
        else
            modeNotIn = Mode.Discrete;

        //Option to switch between modes
        super.setItem((0 * 9) - 1 + 5,
                Utils.createItem(Material.LEVER, 1,
                        TutorialGUIUtils.optionTitle("You are in "+mode +" mode"),
                        TutorialGUIUtils.optionLore("Click to switch to "+modeNotIn + " mode")),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        //Switches the mode
                        mode = modeNotIn;
                        //Refreshes the menu
                        saveAndCheck();
                        open(user.player);
                    }
                });

        //Adds the numerical or discrete menu items
        switch (mode)
        {
            case Discrete -> addChatItemsDiscrete();
            case Numerical -> addChatItemsNumerical();
        }
    }

    /**
     * Adds the discrete chat items to the menu
     */
    private void addChatItemsDiscrete()
    {
        //Top row will always be the one to switch between discrete and numerical

        //2nd row will be the items/books
        int iNumOptions = answerBooks.size();

        //Recalculate pages required - including for the add item option
        iPages = 1 + ( (iNumOptions)/9 );

        //Puts the player back a page - for instance if they have just deleted an item and the num pages has reduced
        if (iCurrentPage > iPages)
            iCurrentPage = iPages;

        //Page switching

        //Page forwards button
        if (iCurrentPage != iPages)
        {
            super.setItem(6,
                    Utils.createItem(Material.BOOK, 1,
                            TutorialGUIUtils.optionTitle("Page forwards")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            iCurrentPage = iCurrentPage + 1;
                            open(user.player);
                        }
                    });
        }

        //Page back button
        if (iCurrentPage != 1)
        {
            super.setItem(2,
                    Utils.createItem(Material.BOOK, 1,
                            TutorialGUIUtils.optionTitle("Page back")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            iCurrentPage = iCurrentPage - 1;
                            open(user.player);
                        }
                    });
        }

        //The page that we are adding the options for

        int i;
        int iStart = (iCurrentPage-1)*9;
        int iEnd;

        //If we are making the last page, only go up to the final option
        if (iCurrentPage == iPages)
            iEnd = iNumOptions;
        else
            //Else provide 9 options to fill the row
            iEnd = iStart+9;

        //Add the discrete options
        for (i = iStart ; i < iEnd ; i++)
        {
            // ---- Answer ----

            TextEditorBookListener answerBook = answerBooks.get(i);
            String szText = ((TextComponent) ((BookMeta) answerBook.getBook().getItemMeta()).pages().get(0)).content();

            //First item we want at slot 9 (which is row 2 column 1). i-iStart will be 0 for first item.
            int finalI = i;
            super.setItem(9 + i-iStart,
                    Utils.createItem(Material.BOOK, 1,
                            TutorialGUIUtils.optionTitle(szText),
                            TutorialGUIUtils.optionLore("Click to edit")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            //Give player the book and register the book close listener
                            answerBook.startEdit("Answer "+ (finalI+1));

                            user.player.sendMessage(Display.colouredText("Use the book in your inventory to edit the answer for this option", NamedTextColor.GREEN));
                        }
                    });


            if (i != 0) //Cannot delete option 1 or change the score on option 1
            {
                // ---- Score ----
                TextEditorBookListener scoreBook = scoreBooks.get(i);
                String szScore = ((TextComponent) ((BookMeta) scoreBook.getBook().getItemMeta()).pages().get(0)).content();

                //First item we want at slot 18 (which is row 3 column 1). i-iStart will be 0 for first item.
                super.setItem(18 + i-iStart,
                        Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                                TutorialGUIUtils.optionTitle(szScore),
                                TutorialGUIUtils.optionLore("Click to edit")),
                        new GuiAction() {
                            @Override
                            public void click(InventoryClickEvent event) {
                                //Give player the book and register the book close listener
                                scoreBook.startEdit("Score "+ (finalI+1));

                                user.player.sendMessage(Display.colouredText("Use the book in your inventory to edit the score for this option", NamedTextColor.GREEN));
                            }
                        });

                // ---- Delete ----
                //First item we want at slot 27 (which is row 3 column 1). i-iStart will be 0 for first item.
                super.setItem(27 + i-iStart,
                        Utils.createItem(Material.BARRIER, 1,
                                TutorialGUIUtils.optionTitle("Delete Option"),
                                TutorialGUIUtils.optionLore("Click to delete option")),
                        new GuiAction() {
                            @Override
                            public void click(InventoryClickEvent event) {
                                deleteDiscreteItem(finalI);
                            }
                        });
            }
        }

        //Add the + button - will add the option then refresh. It won't open it.
        if (iCurrentPage == iPages)
        {
            int iLocationOfAdd = iNumOptions%9 + 9;

            super.setItem(iLocationOfAdd,
                    Utils.createItem(Material.WRITABLE_BOOK, 1,
                            TutorialGUIUtils.optionTitle("New Option"),
                            TutorialGUIUtils.optionLore("Click to add")),
                    new GuiAction() {
                        @Override
                        public void click(InventoryClickEvent event) {
                            //Adds a new option
                            addDiscreteItem((iNumOptions+1) +"");

                        }
                    });
        }

        //5th row holds the standard options
    }

    /**
     * Adds the numerical chat items to the menu
     */
    private void addChatItemsNumerical()
    {
        //Top row will always be the one to switch between discrete and numerical
        //2nd row can be blank
        //3rd row can be the min (3), target (5) and max (7) options

        String szMin = ((TextComponent) ((BookMeta) numericalMin.getBook().getItemMeta()).pages().get(0)).content();
        String szTarget = ((TextComponent) ((BookMeta) numericalPerfect.getBook().getItemMeta()).pages().get(0)).content();
        String szMax = ((TextComponent) ((BookMeta) numericalMax.getBook().getItemMeta()).pages().get(0)).content();

        super.setItem(20,
                Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                        TutorialGUIUtils.optionTitle("Minimum"),
                        TutorialGUIUtils.optionLore(szMin)),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        //Give player the book and register the book close listener
                        numericalMin.startEdit("Minimum");

                        user.player.sendMessage(Display.colouredText("Use the book in your inventory to edit the minimum acceptable value for this option", NamedTextColor.GREEN));
                    }
                });

        super.setItem(22,
                Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                        TutorialGUIUtils.optionTitle("Target"),
                        TutorialGUIUtils.optionLore(szTarget)),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        //Give player the book and register the book close listener
                        numericalPerfect.startEdit("Target");

                        user.player.sendMessage(Display.colouredText("Use the book in your inventory to edit the target value for this option", NamedTextColor.GREEN));
                    }
                });

        super.setItem(24,
                Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                        TutorialGUIUtils.optionTitle("Maximum"),
                        TutorialGUIUtils.optionLore(szMax)),
                new GuiAction() {
                    @Override
                    public void click(InventoryClickEvent event) {
                        //Give player the book and register the book close listener
                        numericalMax.startEdit("Maximum");

                        user.player.sendMessage(Display.colouredText("Use the book in your inventory to edit the maximum acceptable value for this option", NamedTextColor.GREEN));
                    }
                });

        //4th row can be blank
        //5th row holds the standard options
    }


    /**
     * Performs all the logic necessary to add a new discrete option to the list, and refresh
     */
    private void addDiscreteItem(String szNumber)
    {
        //Create answer book
        TextEditorBookListener newItem = new TextEditorBookListener(plugin, user.player, this, "Answer " +szNumber,
               new AnswerBookClose(user, this, false, false),
                "");

        //Add book to list
        answerBooks.add(newItem);

        //Create score book
        TextEditorBookListener firstItem = new TextEditorBookListener(plugin, user.player, this, "Score "+ szNumber,
                new AnswerBookClose(user, this, true, true),
                "1");

        //Add book to list
        scoreBooks.add(firstItem);

        open(user.player);
    }

    /**
     * Performs all the logic necessary to delete a discrete option from the list, and refresh
     */
    private void deleteDiscreteItem(int iIndex)
    {
        //Delete answer book from list
        answerBooks.remove(iIndex);

        //Delete score book from list
        scoreBooks.remove(iIndex);

        open(user.player);
    }

    /**
     * Saves the information in the LocationTask and checks whether the information is all set and we are ready to move on
     */
    public void saveAndCheck()
    {
        //Stores the location task information
        switch (this.mode)
        {
            case Mode.Discrete:
                int iNumAnswers = answerBooks.size();
                String szAnswer = "Discrete:";
                int i;
                for (i = 0 ; i < iNumAnswers - 1 ; i++)
                {
                    szAnswer = szAnswer + ((TextComponent) ((BookMeta) answerBooks.get(i).getBook().getItemMeta()).pages().get(0)).content() +","
                            + ((TextComponent) ((BookMeta) scoreBooks.get(i).getBook().getItemMeta()).pages().get(0)).content() +";";
                }
                szAnswer = szAnswer + ((TextComponent) ((BookMeta) answerBooks.get(i).getBook().getItemMeta()).pages().get(0)).content() +","
                        + ((TextComponent) ((BookMeta) scoreBooks.get(i).getBook().getItemMeta()).pages().get(0)).content();

                super.locationTask.setAnswers(szAnswer);

                //Note because 1 is always set to be full performance we should be good. Do we always set this at the start tho or do we require some setting of the option first?
                this.taskFullySet();

                break;

            case Mode.Numerical:
                float fMinimum = Float.parseFloat(((TextComponent) ((BookMeta) (numericalMin.getBook().getItemMeta())).pages().get(0)).content());
                float fTarget = Float.parseFloat(((TextComponent) ((BookMeta) (numericalPerfect.getBook().getItemMeta())).pages().get(0)).content());
                float fMaximum = Float.parseFloat(((TextComponent) ((BookMeta) (numericalMax.getBook().getItemMeta())).pages().get(0)).content());

                if (fMinimum > fTarget || fTarget > fMaximum)
                {
                    this.taskNoLongerReadyToMoveOn();
                }
                else
                {
                    this.taskFullySet();
                }

                String szAnswerNum = "Numerical:"+fMinimum+","+fTarget+","+fMaximum;
                super.locationTask.setAnswers(szAnswerNum);
        }
    }
}

//Design this to deal with scores and answers
class AnswerBookClose implements BookCloseAction
{
    private final User user;
    private final boolean bRequireFloat;
    private final boolean bRequireFraction;

    //The parent menu to open when book closed
    private final ChatLocationTaskEditorMenu chatEditMenu;

    public AnswerBookClose(User user, ChatLocationTaskEditorMenu chatEditMenu, boolean bRequireFloat, boolean bRequireFraction)
    {
        this.user = user;
        this.chatEditMenu = chatEditMenu;
        this.bRequireFloat = bRequireFloat;
        this.bRequireFraction = bRequireFraction;
    }

    /**
     * @param oldBookMeta            The previous metadata of the book just closed.
     * @param newBookMeta            The new metadata of the book just closed.
     * @param textEditorBookListener A reference to the book listener itself which calls this. Enables unregistering to be called
     * @param szNewContent           The combined content of all pages in the new book. This is always provided for convenience
     * @return Whether to accept the input and save the text
     */
    @Override
    public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent)
    {
        //Deal with the answer
        //We nothing rellly. The answer already gets stored cos it's in the book now. When they close the book it's all done, we just want to show them the menu again
        //And take the book away from them.

        //Unregister the listener
        textEditorBookListener.unregister();

        //Remove the book
        user.player.getInventory().getItemInMainHand().setAmount(0);

        //Verify the input
        boolean bVerificationPassed;
        if (bRequireFloat)
        {
            try
            {
                float fScore = Float.parseFloat(szNewContent);

                //Passed the float step

                //Checks whether it is out of range
                if (bRequireFraction)
                {
                    if (fScore < 0 || fScore > 1)
                    {
                        user.player.sendMessage(Display.errorText("You must enter something in the range 0-1"));
                        bVerificationPassed = false;
                    }
                    else
                    {
                        bVerificationPassed = true;
                    }
                }
                else
                    bVerificationPassed = true;
            }
            catch (NumberFormatException e)
            {
                user.player.sendMessage(Display.errorText("You must enter a floating point number in the range 0-1"));
                bVerificationPassed = false;
            }

            return bVerificationPassed;
        }

        //Always pass
        else
            return true;
    }

    @Override
    public boolean runBookSign(BookMeta bookMeta, BookMeta bookMeta1, TextEditorBookListener textEditorBookListener, String s) {
        return runBookClose(bookMeta, bookMeta1, textEditorBookListener, s);
    }

    @Override
    public void runPostClose()
    {
        //Refresh and reopen the menu
        chatEditMenu.saveAndCheck();
        chatEditMenu.open(user.player);
    }
}

//Design this to deal with numericals
class NumericalBookClose implements BookCloseAction
{
    private final User user;

    //The parent menu to open when book closed
    private final ChatLocationTaskEditorMenu chatEditMenu;

    private final Numerical numerical;

    public NumericalBookClose(User user, ChatLocationTaskEditorMenu chatEditMenu, Numerical numerical)
    {
        this.user = user;
        this.chatEditMenu = chatEditMenu;
        this.numerical = numerical;
    }

    /**
     * @param oldBookMeta            The previous metadata of the book just closed.
     * @param newBookMeta            The new metadata of the book just closed.
     * @param textEditorBookListener A reference to the book listener itself which calls this. Enables unregistering to be called
     * @param szNewContent           The combined content of all pages in the new book. This is always provided for convenience
     * @return Whether to accept the input and save the text
     */
    @Override
    public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent)
    {
        //Unregister the listener
        textEditorBookListener.unregister();

        //Remove the book
        user.player.getInventory().getItemInMainHand().setAmount(0);

        //Verify the input
        boolean bVerificationPassed;
        float fNewNumber;

        //Verify it is a number
        try
        {
            fNewNumber = Float.parseFloat(szNewContent);
        }
        catch (NumberFormatException e)
        {
            user.player.sendMessage(Display.errorText("You must enter a number"));
            return false;
        }

        //Verify that the limits are sound
        switch (numerical)
        {
            case Min:
                if (fNewNumber > chatEditMenu.getNumericalTarget())
                {
                    user.player.sendMessage(Display.errorText("The minimum cannot be greater than the target"));
                    return false;
                }
                break;
            case Target:
                if (fNewNumber < chatEditMenu.getNumericalMin())
                {
                    user.player.sendMessage(Display.errorText("The target cannot be less than the minimum"));
                    return false;
                }
                if (fNewNumber > chatEditMenu.getNumericalMax())
                {
                    user.player.sendMessage(Display.errorText("The target cannot be greater than the maximum"));
                    return false;
                }
                break;
            case Max:
                if (fNewNumber < chatEditMenu.getNumericalTarget())
                {
                    user.player.sendMessage(Display.errorText("The maximum cannot be less than the target"));
                    return false;
                }
                break;
        }
        return true;
    }

    public boolean runBookSign(BookMeta bookMeta, BookMeta bookMeta1, TextEditorBookListener textEditorBookListener, String s) {
        return runBookClose(bookMeta, bookMeta1, textEditorBookListener, s);
    }

    public void runPostClose()
    {
        //Refresh and reopen the menu
        chatEditMenu.saveAndCheck();
        chatEditMenu.open(user.player);
    }
}

enum Mode
{
    Discrete, Numerical
}

enum Numerical
{
    Min, Target, Max
}
