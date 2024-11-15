import java.awt.image.BufferedImage;
import java.util.Random;

import fromics.Background;
import fromics.Frindow;
import fromics.Manager;


//only the WaveFunctionCollapser is at all commented, since it's the only important class here
//this uses my custom java library for its graphics, so see that if you want to know hoe it works,
//though the GitHub for it is probably out of date since I forget to commit and push things
public class Main extends Manager {
    public static final int WIDTH = WaveFunctionCollapser.WIDTH * WaveFunctionCollapser.TILE_WIDTH;
    public static final int HEIGHT = WaveFunctionCollapser.HEIGHT * WaveFunctionCollapser.TILE_HEIGHT;
    public static final Random GLOBAL_RAND = new Random((long)(Math.random() * 100000));

    public static final boolean VISIBLE = true;

    private Frindow win;

    public static void main(String[] args) {
        if(VISIBLE) {
            new Main(new Frindow(BufferedImage.TYPE_INT_RGB, (int) (WIDTH) + 20, (int) (HEIGHT) + 45));
        } else {
            WaveFunctionCollapser wfc = new WaveFunctionCollapser(new Frindow(BufferedImage.TYPE_INT_RGB, 0, 0));
            while(!wfc.isDone()) {
                wfc.update();
            }
            wfc.close();
        }
    }

    public Main(Frindow observer) {
        super(observer, 20, 0);

        win = observer;
        screens = new Background[1];
        initScreen(0);
        win.init(1, this);
        startVariableLoop();
    }

    @Override
    public void close() {
        screens[0].close();
    }

    @Override
    protected void initScreen(int n) {
        screens[0] = new WaveFunctionCollapser(win);
        link(screens[0]);
    }

}
