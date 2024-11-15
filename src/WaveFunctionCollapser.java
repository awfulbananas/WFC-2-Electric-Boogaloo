import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

import fromics.Background;
import fromics.Frindow;
import fromics.Point;

public class WaveFunctionCollapser extends Background {
    //number of tiles in a row
    public static final int WIDTH = 1000;
    //width of a single tile in pixels
    public static final int TILE_WIDTH = 3;
    //number of tiles in a column
    public static final int HEIGHT = 1000;
    //height of a single tile in pixels
    public static final int TILE_HEIGHT = 3;
    //number of sides of a tile
    public static final int TILE_SIDES = 4;

    //an array containing all of the tile types for this wave function collapse
    private TileType[] allTypes;

    private Tile[][] board;
    private Set<Tile> allCollapsed;
    private TreeSet<Tile> choosableTiles;
    private int collapsedTiles;
    private BufferedImage img;
    private Graphics g;
    private boolean imgSaved;

    public WaveFunctionCollapser(Frindow observer) {
        super(observer);
        setX(0);
        setY(0);
		loadTypes("resources/coast/coastTiles.tiles");

        allCollapsed = new HashSet<>();
        choosableTiles = new TreeSet<>();
        board = new Tile[WIDTH][HEIGHT];
        collapsedTiles = 0;
        for(int x = 0; x < WIDTH; x++) {
            for(int y = 0; y < HEIGHT; y++) {
                Tile newTile = new Tile(allTypes, x, y);
                board[x][y] = newTile;
                newTile.recalculateEntropy();
            }
        }

        img = new BufferedImage(WIDTH * TILE_WIDTH, HEIGHT * TILE_HEIGHT, BufferedImage.TYPE_3BYTE_BGR);
        g = img.getGraphics();
        imgSaved = false;
        int x = Main.GLOBAL_RAND.nextInt(WIDTH);
        int y = Main.GLOBAL_RAND.nextInt(HEIGHT);
        collapseTarget(x, y);
        System.out.println("possible collapses: " + choosableTiles.size());
    }


    //spec for .tiles format:
    //each line may have either a valid field, or any other text, except that that text may not contain a ':'.
    //lines with other text will be ignored
    //a valid field consists of a valid field name, followed by a ':' followed by the value of the field
    //fields:
    //  type:
    //      a string, not containing a ':' or a line break, which determines the name of the tile type
    //  file:
    //      a string which represents either a, the file path of the texture of the tile relative to the program,
    //      or b, a full file path pointing to the texture of the tile
    //  weight:
    //      should be a string representing an integer number in ascii characters
    //      determining the weight of the tile when picking tiles
    //  sockets:
    //      should be a list of lists denoting the integer sockets of each side of the tile.
    //      there should be exactly four sockets, separated by the '|' character
    //      each socket can be any number of integers, each separated by the '-' character
    //      each integer should be represented by ascii characters
    //      no characters not denoting the socket should be present in the field
    //  rotations:
    //      a list of single character integers ranging from 0-3 inclusive, each separated by the '|' character
    //      each integer represents a possible rotation of the tile, with the integer representing the number of
    //      90 degree counter-clockwise rotations from the base tile
    //      if the rotations field is included, a 0 rotation must be present in the list to include the base rotation
    //      each rotation is considered a separate tile for weights, so if you want the total weight of all of a tile's
    //      rotations to be the same as a tile with no rotations, the tile with no rotations must have a higher weight
    //  end:
    //      the value of this field may be anything, but it may be helpful to denote the separation between tiles
    //      on this line.
    //      the end field denotes the end of defining a tile, and moving on to the next tile
    //  reflections:
    //      not implemented yet, so won't do anything
    //
    //if a field is repeated within the same tile definition, the second instance of the field is used
    //
    //there are some exceptions to this for ease of parsing.
    //the most important is that a rotations definition will set the defined rotations to the most recently defined sockets within
    //the tile definition, meaning that rotation definitions will overlap rather than override completely
    //
    //certain fields have default values, and may be skipped within a tile definition.
    //the default value for type is "def", standing for default
    //the default value for weight is 1
    //the default value for file is "resources\xPipe.png" which points to an image similar to a plus sign
    //rotations doesn't have a default value, but is rotations aren't defined, but rotations may be skipped
    // so that only rotation 0 will be made
    //sockets also has no default value, but may not be skipped
    //end doesn't have a default value, due to the value not mattering, but the end field must always be included for
    // each tile definition
    private void loadTypes(String filePath) {
        File inputFile = new File(filePath);
        Scanner in = null;
        try {
            in = new Scanner(inputFile);
        } catch (FileNotFoundException e) {
            System.out.println("failed to load tiles file: " + e);
        }

        ArrayList<TileType> typeList = new ArrayList<>();

        int[][] curSockets = null;
        int[][][] rotations = null;
        int[][][] reflections = null;
        int weight = 1;
        String typeName = "def";
        String texturePath = "resources/xPipe.png";

        assert in != null;
        while(in.hasNext()) {
            String[] line = in.nextLine().split(":");

            String dataType = line[0];
            String data = line[1];
            System.out.println(dataType + ":  " + data);

            if(line.length > 1) {
                switch(dataType) {
                    case "type":
                        typeName = data;
                        break;
                    case "file":
                        texturePath = data.replace('\\','/');
                        break;
                    case "weight":
                        weight = Integer.parseInt(data);
                        break;
                    case "sockets":
                        //splits sockets by "|" characters
                        String[] newSockets = data.split("\\|");
                        //creates array of sockets
                        curSockets = new int[newSockets.length][];
                        //for each socket being initialized
                        for(int i = 0; i < curSockets.length; i++) {
                            //pieces of sockets are split by "-" characters
                            String[] newSocketStrings = newSockets[i].split("-");
                            //creates the singular socket array
                            int[] newSocket = new int[newSocketStrings.length];
                            //populates the given socket with the parsed info
                            for(int j = 0; j < newSocket.length; j++) {
                                newSocket[j] = Integer.parseInt(newSocketStrings[j]);
                            }
                            //puts the socket into the array of sockets
                            curSockets[(curSockets.length - i) % curSockets.length] = newSocket;
                        }
                        break;
                    case "rotations":
                        //gets strings for each rotation of the tile
                        String[] rotationStrings = data.split("\\|");
                        //create an array to hold the rotated sockets, size is number of tile sides so direction of rotation can be easily parsed
                        rotations = new int[TILE_SIDES][][];
                        //for each rotation
                        for(int i = 0; i < rotationStrings.length; i++) {
                            //parse the rotation number
                            int curRotation = Integer.parseInt(rotationStrings[i]);
                            //create an array for the rotated sockets
                            assert curSockets != null;//(it isn't as long as the file is formatted correctly)
                            int[][] rotatedSockets = new int[curSockets.length][];
                            //for each socket
                            for(int j = 0; j < curSockets.length; j++) {
                                //assign the socket to a rotation of the previous sockets
                                rotatedSockets[j] = curSockets[((j - curRotation) % curSockets.length + curSockets.length) % curSockets.length];
                            }
                            //add the new sockets to an array
                            rotations[(rotations.length-curRotation) % rotations.length] = rotatedSockets;
                        }
                        break;
                    case "end":
                        if(rotations == null) {
                            //if no rotations were defined, create rotation 0
                            typeList.add(new TileType(curSockets, typeName, texturePath, 0, weight));
                        } else {
                            //if any rotations were defined, create a tile type for each rotation that was defined
                            for(int i = 0; i < TILE_SIDES; i++) {
                                if(rotations[i] != null) {
                                    //the name is appended with the rotation, for ease of debugging
                                    typeList.add(new TileType(rotations[i], typeName + "R" + i, texturePath, i, weight));
                                }
                            }
                        }

                        curSockets = null;
                        rotations = null;
                        reflections = null;
                        weight = 1;
                        typeName = "def";
                        texturePath = "resources/xPipe.png";
                        break;
                    default:
                        break;
                }
            } else {
                System.out.println("Read Line Failed");
            }
        }

        //once tile types are parsed, make them an array, since the number of types should never change
        allTypes = typeList.toArray(new TileType[typeList.size()]);

        //calculate the valid neighbors for each side of each tile, for each tile
        //(I really should optimize this using the symmetries of this operation, but it's not urgent)
        for(TileType type : allTypes) {
            type.calculateSocketValues(allTypes);
        }
    }

    //outputs the created image when the program is closed
    //also means this could be run without a window to be a bit faster, and still give output
    public void close() {
        System.out.println("closing");
        (new Thread(() -> {
            try {
                imgSaved = true;
                ImageIO.write(img, "png", new File("output.png"));
            } catch (IOException e) {
                System.out.println("failed to save final image");
                e.printStackTrace();
            }
        })).start();
    }

    @Override
    public void onFirstLink() {
        addKeystrokeFunction((KeyEvent e) -> {
            //when space is pressed
            if(e.getKeyCode() == KeyEvent.VK_SPACE) {
                //collapse the next tile
                collapseNext();
                System.out.println(choosableTiles.size());
            }
            if(e.getKeyCode() == KeyEvent.VK_S) {
                close();
            }
            if(e.getKeyCode() == KeyEvent.VK_N) {
                reset();
            }
        });
    }

    private void reset() {
        allCollapsed = new HashSet<>();
        choosableTiles = new TreeSet<>();
        board = new Tile[WIDTH][HEIGHT];
        collapsedTiles = 0;
        for(int x = 0; x < WIDTH; x++) {
            for(int y = 0; y < HEIGHT; y++) {
                Tile newTile = new Tile(allTypes, x, y);
                board[x][y] = newTile;
                newTile.recalculateEntropy();
            }
        }
        img = new BufferedImage(WIDTH * TILE_WIDTH, HEIGHT * TILE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        g = img.getGraphics();
        imgSaved = false;
        int x = Main.GLOBAL_RAND.nextInt(WIDTH);
        int y = Main.GLOBAL_RAND.nextInt(HEIGHT);
        collapseTarget(x, y);
    }

    @Override
    public boolean update() {
        if(!imgSaved) {
            if (getKey(KeyEvent.VK_UP)) {
                add(0, -0.001*dt());
            }
            if (getKey(KeyEvent.VK_DOWN)) {
                add(0, 0.001*dt());
            }
            if (getKey(KeyEvent.VK_LEFT)) {
                add(-0.001*dt(), 0);
            }
            if (getKey(KeyEvent.VK_RIGHT)) {
                add(0.001*dt(), 0);
            }
        } else {
            if (getKey(KeyEvent.VK_UP)) {
                add(0, -0.0001);
            }
            if (getKey(KeyEvent.VK_DOWN)) {
                add(0, 0.0001);
            }
            if (getKey(KeyEvent.VK_LEFT)) {
                add(-0.0001, 0);
            }
            if (getKey(KeyEvent.VK_RIGHT)) {
                add(0.0001, 0);
            }
        }
        setX((X() + Main.WIDTH) % Main.WIDTH);
        setY((Y() + Main.WIDTH) % Main.HEIGHT);

        //if all tiles have been collapsed, then don't collapse any more tiles
        if(collapsedTiles >= WIDTH * HEIGHT) {
            //if all tiles are collapsed and no image has been saved, save an image
            if(!imgSaved)close();
            return false;
        }
        collapseNext();
        return false;
    }

    private boolean collapseTarget(int x, int y) {
        boolean val =  collapseTile(board[x][y]);
        //System.out.println("collapsed (" + x + ", " + y + ") to " + board[x][y].getCollapsedType().toString());
        //System.out.println("next possibilities are " + choosableTiles.toString());
        return val;
    }

    private boolean collapseNext() {
        if(!choosableTiles.isEmpty()) {
            return collapseTile(choosableTiles.pollFirst());
        } else {
            close();
            System.out.println("WFC failure");
            return false;
        }
    }

    private boolean collapseTile(Tile t) {
        if(t.isCollapsed()) {
            System.out.println("--Fatal Error--");
            return false;
        } else {
            try {
                t.collapse(g);
                // System.out.println("collapsed: " + t.getCollapsedType());
                collapsedTiles++;
                allCollapsed.add(t);
                Set<Tile> origin = new HashSet<>();
                origin.add(t);
                TreeSet<Tile> originList = new TreeSet<>();
                originList.add(t);
                propagate(origin, originList);
                System.out.println(collapsedTiles);
                return true;
            } catch(IllegalArgumentException e) {
                reset();
                return false;
            }
        }
    }

    public boolean isDone() {
        return collapsedTiles >= WIDTH * HEIGHT;
    }

    private void propagate(Set<Tile> narrowedTiles, TreeSet<Tile> narrowingTiles){
        //until you run out of tiles which can further limit possibilities
        while(!narrowingTiles.isEmpty()) {
            //get the tile with the lowest entropy
            Tile cur = narrowingTiles.pollFirst();
            assert cur != null;//(it isn't)
            //get the tiles directly neighboring this one
            Tile[] affectees = getOrthogonal(cur);
            //for each neighbor
            for(int i = 0; i < TILE_SIDES; i++) {
                //t is the current neighbor
                Tile t = affectees[i];
                //skip the neighbor if it's already been as narrowed as possible
                if(narrowedTiles.contains(t)) {
                    continue;
                }
                //get the number of possibilities of the neighbor before being limited
                int preLimitSize = t.possibilityCount();
                //if it's already collapsed or it only has one possibility
                if(t.isCollapsed() || preLimitSize == 1) {
                    //add it to the list of tiles which are as narrowed as possible
                    narrowedTiles.add(t);
                    continue;
                }
                //limit the neighbor
                t.limit(cur.getPossibleTiles(i));
                //if the neighbor was at all limited,
                if(t.possibilityCount() < preLimitSize) {
                    //remove it from the list of choosable tiles before changing values that would affect it's sorting order
                    //(or do nothing if it's not in the set)
                    choosableTiles.remove(t);
                    //recalculate the entropy of the tile
                    t.recalculateEntropy();
                    //add it to the list of tiles which might further limit their neighbors
                    narrowingTiles.add(t);
                    //add it to the list of tiles to be collapsed
                    choosableTiles.add(t);
                }
            }
        }

    }





    private Tile[] getOrthogonal(Tile t) {
        Tile[] orthogonal = new Tile[4];
        orthogonal[0] = board[((int)t.X() + 1) % WIDTH][(int)t.Y()];
        orthogonal[1] = board[(int)t.X()][((int)t.Y() + 1) % HEIGHT];
        orthogonal[2] = board[((int)t.X() - 1 + WIDTH) % WIDTH][(int)t.Y()];
        orthogonal[3] = board[(int)t.X()][((int)t.Y() - 1 + HEIGHT) % HEIGHT];
        return orthogonal;
    }

    private Set<Tile> getOrthogonalSet(Tile t) {
        Set<Tile> orthogonal = new TreeSet<>();
        Tile right = board[((int)t.X() + 1) % WIDTH][(int)t.Y()];
        if(!(right.isCollapsed())) orthogonal.add(right);
        Tile down = board[(int)t.X()][((int)t.Y() + 1) % HEIGHT];
        if(!(down.isCollapsed())) orthogonal.add(down);
        Tile left= board[((int)t.X() - 1 + WIDTH) % WIDTH][(int)t.Y()];
        if(!(left.isCollapsed())) orthogonal.add(left);
        Tile up = board[(int)t.X()][((int)t.Y() - 1 + HEIGHT) % HEIGHT];
        if(!(up.isCollapsed())) orthogonal.add(up);
        return orthogonal;
    }

    private void compareLists(List<TileType> mainList, List<TileType> comparisonList) {
        Iterator<TileType> tItr = mainList.iterator();
        while(tItr.hasNext()) {
            if(!comparisonList.contains(tItr.next())) {
                tItr.remove();
            }
        }
    }

    @Override
    protected void draw(Graphics g, BufferedImage img, double xOff, double yOff, double angOff) {
        double curXOff = X();
        double curYOff = Y();
        for(int x = 0; x < img.getWidth(); x++) {
            for(int y = 0; y < img.getHeight(); y++) {
                img.setRGB(x, y, this.img.getRGB((int)((x + curXOff) % Main.WIDTH), (int)((y + curYOff) % Main.HEIGHT)));
            }
        }
    }

}
