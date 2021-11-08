import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        launch("4");
        int r2 = 0xFD;
        int r3 = 0x9B;
        System.out.println(Controller.hex(r2 + r3));
    }

    static void launch(String name) throws FileNotFoundException {
        String task = scan(name);
        String[] parts = task.split("\n\n");
        Controller c = new Controller(parts[0], parts[1], parts[2]);

        c.run();
    }

    static void prepare(String original) {
        System.out.println(original.replaceAll("  ", " ").replaceAll("\t", ""));
    }

    static String scan(String name) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File("src/vars/" + name));
        String text = scanner.useDelimiter("\\A").next();
        scanner.close();

        return text;
    }
}
