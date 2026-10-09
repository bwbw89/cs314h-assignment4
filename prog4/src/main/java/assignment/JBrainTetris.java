package assignment;

public class JBrainTetris extends JTetris {
    private Brain brain;

    // the entire idea of speed and ticks is to have further fine tune 
    // control over the speed of the program (be able to slow it down more
    // action by action)

    // number of timer ticks between brain moves
    private int speed;
    // ticks left until the brain moves again
    private int ticks;


    public JBrainTetris(boolean smart, int tick) {
        super();
        speed = ticks = tick;

        // IntelligentBrain if smart, otherwise LameBrain
        brain = (smart) ? new IntelligentBrain() : new LameBrain();
    }

    @Override
    public void tick(Board.Action verb) {
        // only replace the timer's DOWN, never a key press
        if (gameOn && verb == Board.Action.DOWN) {

            ticks--;

            // brain's move takes the place of DOWN
            if (ticks == 0) {
                verb = brain.nextMove(board);
                ticks = speed;
            }
        }

        super.tick(verb);
    }

    public static void main(String[] args) {
        // smart brain, moving every tick; tick of 1 lowest val
        JBrainTetris brainTetris = new JBrainTetris(true, 1);

        createGUI(brainTetris);
    }

}