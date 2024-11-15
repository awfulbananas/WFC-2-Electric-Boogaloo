import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.*;

import fromics.Background;
import fromics.Point;
import fromics.Texture;

public class Tile extends Point implements Comparable<Tile>{
    private final List<TileType> possibleTypes;
    private TileType collapsedType;
    private final TileType[] allTypes;
    private double size;
    private double entropy;
    //used to break sorting ties
    private final double tieBreaker;

    public Tile(TileType[] allTypes, int x, int y) {
        super();

        setX(x);
        setY(y);
        size = (double) Main.WIDTH / WaveFunctionCollapser.WIDTH;

        tieBreaker = Main.GLOBAL_RAND.nextDouble();
        this.allTypes = allTypes;
        possibleTypes = new LinkedList<>();
        collapsedType = null;



        Collections.addAll(possibleTypes, allTypes);
    }

    public Set<TileType> getPossibleTiles(int dir) {
        Set<TileType> possibilities = new HashSet<>();
        if(collapsedType == null) {
            for(TileType possibleType : possibleTypes) {
                for(TileType type : allTypes) {
                    if(possibleType.canSocket(type, dir)) {
                        possibilities.add(type);
                    }
                }
            }
        } else {
            for(TileType type : allTypes) {
                if(collapsedType.canSocket(type, dir)) {
                    possibilities.add(type);
                }
            }
        }
        return possibilities;
    }

    public void limit(Set<TileType> possibilities) {
        Iterator<TileType> tItr = possibleTypes.iterator();
        while(tItr.hasNext()) {
            if(!possibilities.contains(tItr.next())) {
                tItr.remove();
            }
        }
    }

    public int possibilityCount() {
        return possibleTypes.size();
    }

    public List<TileType> getPossibleTypes() {
        return possibleTypes;
    }

    public void collapse(Graphics imgGraphics) {
        int totalWeight = 0;
        for(TileType t : possibleTypes) {
            totalWeight += t.getWeight();
        }
        int[] selector = new int[totalWeight];
        for(int i = 0, ind = 0; i < possibleTypes.size(); i++) {
            int weight = possibleTypes.get(i).getWeight();
            for(int j = 0; j < weight; j++, ind++) {
                selector[ind] = i;
            }
        }
        entropy = -1;
        collapsedType = possibleTypes.get(selector[Main.GLOBAL_RAND.nextInt(selector.length)]);
        BufferedImage img = collapsedType.getImage();
        size = Math.max((double)WaveFunctionCollapser.TILE_WIDTH / (double) img.getWidth(), 0);
        imgGraphics.drawImage(img, (int)((X() + 0.5) * (WaveFunctionCollapser.TILE_WIDTH) + 1 - size * img.getWidth() / 2),
                (int)((Y() + 0.5) * (WaveFunctionCollapser.TILE_HEIGHT) + 1 - size * img.getHeight() / 2),
                (int)((X() + 0.5) * (WaveFunctionCollapser.TILE_WIDTH) + 1 + size * img.getWidth() / 2),
                (int)((Y() + 0.5) * (WaveFunctionCollapser.TILE_HEIGHT) + 1 + size * img.getHeight() / 2),
                0, 0, img.getWidth(), img.getHeight(), null);
    }

    public TileType getCollapsedType() {
        return collapsedType;
    }

    public boolean isCollapsed() {
        return collapsedType != null;
    }

    public String toString() {
        return  "(" + X() + ", " + Y() +") --" + possibleTypes.toString() + "--";
    }

    public void recalculateEntropy() {
        int runningSum = 0;
        double runningLogProdSum = 0;
        for(TileType t : possibleTypes) {
            int weight = t.getWeight();
            runningSum += weight;
            runningLogProdSum += (double) weight * Math.log(weight);
        }
        this.entropy = Math.log(runningSum) - (runningLogProdSum / (double)runningSum);
    }

    @Override
    public int compareTo(Tile o) {
        //if it's the same tile, it's the same tile
        if(o == this) {
            return 0;
        }

        //lower entropy tiles are sorted lower
        double entropyCompare = this.entropy - o.entropy;
        if(entropyCompare < 0) {
            return -1;
        } else if(entropyCompare > 0) {
            return  1;
        }

        //use random number tiebreaker if entropy is equal
        double tiebreakerCompare = this.tieBreaker - o.tieBreaker;
        if(tiebreakerCompare < 0) {
            return  -1;
        } else if (tiebreakerCompare > 0) {
            return 1;
        }

        //if by some ungodly coincidence the tiebreaker values are equal, compare locations on the grid
        double xCompare = X() - o.X();
        if(xCompare < 0) {
            return -1;
        } else if(xCompare > 0) {
            return 1;
        }

        double yCompare = Y() - o.Y();
        if(yCompare < 0) {
            return -1;
        } else if(yCompare > 0) {
            return 1;
        }



        return 0;
    }
}
