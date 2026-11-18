import java.io.PrintStream;
import java.util.Scanner;
public class Main {
    public static Scanner in = new Scanner(System.in);
    public static PrintStream out = System.out;
    public static void main(String[] args) {
        int a = in.nextInt();
        int b = in.nextInt();
        int n = 0;
        while(a>0){
            n+=b;
            a+=-1;
        }
        out.print(n);
    }
}