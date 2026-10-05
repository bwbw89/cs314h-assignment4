package assignment;

public class JBrainTetris extends JTetris {
    private Brain brain;
    private int speed;
    private int ticks;


    public JBrainTetris(boolean smart, int tick) {
        super();
        speed = ticks = tick;
        brain = (smart) ? new IntelligentBrain() : new LameBrain();
    }

    @Override
    public void tick(Board.Action verb) {
        if (gameOn && verb == Board.Action.DOWN) {

            ticks--;

            if (ticks == 0) {
                verb = brain.nextMove(board);
                ticks = speed;
            }
        }

        super.tick(verb);
    }
    public static void main(String[] args) {
        JBrainTetris brainTetris = new JBrainTetris(true, 3);
        
        createGUI(brainTetris);
    }

}
