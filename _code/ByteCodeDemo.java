import java.lang.Math;

public class ByteCodeDemo {

    public static int absDifference(int a, int b) {
        int difference = a - b;
        return Math.abs(difference);
    }

    public static void main(String[] args) {
        System.out.println(absDifference(2, 1));
    }
}