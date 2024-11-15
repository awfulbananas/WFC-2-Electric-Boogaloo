import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

import fromics.Texture;

public class TileType implements Comparable<TileType>{
    private final int[][] edgeSockets;
    private final Set<TileType>[] socketValues;
    private final String name;
    private BufferedImage img;
    private final int weight;

    @SuppressWarnings("unchecked")
    public TileType(int[][] edgeSockets, String name, String texturePath, int rotation, int weight) {
        this.edgeSockets = Arrays.copyOf(edgeSockets, edgeSockets.length);
        this.name = name;
        this.weight = weight;

        try {
            img = Texture.getRotatedImage(ImageIO.read(new File(texturePath)), (double)rotation * Math.PI / 2.0);
        } catch (IOException e) {
            System.out.println("failed to load texture: " + texturePath);
            e.printStackTrace();
        }

        socketValues = new Set[4];

        for(int i = 0; i < edgeSockets.length; i++) {
            System.out.println(Arrays.toString(edgeSockets[i]));
        }
        System.out.println();
    }

    public void calculateSocketValues(TileType[] allTypes) {
        for(int i = 0; i < 4; i++) {
            socketValues[i] = new TreeSet<>();

            for(TileType type : allTypes) {
                if(calculateCanSocket(type, i)) socketValues[i].add(type);
            }
        }
    }

    public boolean canSocket(TileType type, int dir) {
        return socketValues[dir].contains(type);
    }

    public Set<TileType> getSocketableTypes(int dir){
        return socketValues[dir];
    }

    public int getWeight() {
        return weight;
    }

    private boolean calculateCanSocket(TileType type, int dir) {
//		System.out.println("________");
//		for(int i = 0; i < edgeSockets.length; i++) {
//			System.out.println(Arrays.toString(edgeSockets[i]) );
//		}
//		System.out.println();

//		for(int i = 0; i < type.edgeSockets.length; i++) {
//			System.out.println(Arrays.toString(type.edgeSockets[i]) );
//		}
//		System.out.println(dir);

        int[] thisSocket = edgeSockets[dir];
//		System.out.println("this: " + Arrays.toString(thisSocket));
        int[] otherSocket = type.edgeSockets[(dir + 2) % 4];
//		System.out.println("other: " + Arrays.toString(otherSocket));

        if(thisSocket.length != otherSocket.length) {
//			System.out.println("false\n________");
            return false;
        }

        for(int i = 0; i < thisSocket.length; i++) {
            if(thisSocket[i] != otherSocket[otherSocket.length - i - 1]) {
//				System.out.println("false\n________");
                return false;
            }
        }
//		System.out.println("true\n________");
        return true;
    }

    @Override
    public String toString() {
        String s = name + ": ";
        for(int i = 0; i < edgeSockets.length; i++) {
            s += Arrays.toString(edgeSockets[i]) + "\n";
        }
        return s;
    }

    public BufferedImage getImage() {
        return img;
    }

    @Override
    public int compareTo(TileType o) {
        int compareA = this.name.compareTo(o.name);
        return compareA;
    }
}
