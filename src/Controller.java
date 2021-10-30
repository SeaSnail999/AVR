import java.util.HashMap;

public class Controller {
    private static class Subprogram {
        int addr;
        String body;

        private Subprogram(String text) {
            String[] parts = text.split("\\)");
            int space = parts[0].indexOf(' ');
            addr = Integer.parseInt(parts[0].substring(space).trim(), 16);
            body = parts[1];
        }

        @Override
        public String toString() {
            return "Subprogram{" +
                    "addr=" + addr +
                    ", body='" + body + '\'' +
                    '}';
        }
    }

    private static class Backup {
        HashMap<String, Byte> registers = new HashMap<>();
        HashMap<Integer, Byte> memory = new HashMap<>();
        int SP;
        int PC = 0x100;
    }

    int PC = 0x100;
    int STACK;
    int SP = 0x25F;

    boolean I, T, H, S, V, N, Z, C;
    String lastCommand;

    int number;

    HashMap<String, Byte> registers = new HashMap<>();
    HashMap<Integer, Byte> memory = new HashMap<>();

    private final String program;
    private final Subprogram subprogram;
    private final Backup backup = new Backup();


    Controller(String memory, String program, String subprogram) {
        loadMemory(memory);
        this.program = program.replace("$100)", "");
        this.subprogram = new Subprogram(subprogram);
    }

    void run() {
        exec(program);
    }

    public static void main(String[] args) {
    }

    private void loadMemory(String memory) {
        String[] parts = memory.split("\n");
        for (String assignment : parts)
            loadVar(assignment);
    }

    private void loadVar(String assignment) {
        String[] parts = assignment.split(" ");
        int key = Integer.parseInt(parts[0], 16);
        byte value = (byte) Integer.parseInt(parts[1].substring(1), 16);
        memory.put(key, value);
    }

    private void exec(String commands) {
        String[] parts = commands.split("\n");
        for (int i = 0; i < parts.length; i++) {
            String command = parts[i];
            execSingle(command.trim());
        }
    }

    private void backup() {
        backup.memory = (HashMap<Integer, Byte>) memory.clone();
        backup.registers = (HashMap<String, Byte>) registers.clone();
        backup.SP = SP;
        backup.PC = PC;
    }

    private void execSingle(String command) {
        backup();
        System.out.println(++this.number + ") Выполнение команды " + command);
        String[] parts = command.split(" ");
        lastCommand = parts[0];
        switch (parts[0]) {
            case "LDS" -> LDS(parts[1].replaceAll(",", ""), Integer.parseInt(parts[2], 16));
            case "ADD" -> ADD(parts[1].replaceAll(",", ""), parts[2].replaceAll(",", ""));
            case "ROL" -> ROL(parts[1].replaceAll(",", ""));
            case "LSR" -> LSR(parts[1].replaceAll(",", ""));
            case "MUL" -> MUL(parts[1].replaceAll(",", ""), parts[2].replaceAll(",", ""));
            case "AND" -> AND(parts[1].replaceAll(",", ""), parts[2].replaceAll(",", ""));
            case "SUB" -> SUB(parts[1].replaceAll(",", ""), parts[2].replaceAll(",", ""));
            case "ICALL" -> {
                ICALL();
                return;
            }
            case "SWAP" -> SWAP(parts[1].replaceAll(",", ""));
            case "SBC" -> SBC(parts[1].replaceAll(",", ""), parts[2].replaceAll(",", ""));
            case "RET" -> RET();
            case "OR" -> OR(parts[1].replaceAll(",", ""), parts[2].replaceAll(",", ""));
            case "EOR" -> EOR(parts[1].replaceAll(",", ""), parts[2].replaceAll(",", ""));
            default -> throw new Error("Command not implemented");
        }
        System.out.println(this);
    }

    private void OR(String Rd, String Rr) {
        PC += 1;
        int rd = registers.get(Rd);
        int rr = registers.get(Rr);

        byte R = (byte) (rd | rr);

        V = false;
        N = (R & 128) != 0;
        S = N ^ V;
        Z = R == 0;

        registers.put(Rd, R);
    }

    private void EOR(String Rd, String Rr) {
        PC += 1;
        int rd = registers.get(Rd);
        int rr = registers.get(Rr);

        byte R = (byte) (rd ^ rr);

        V = false;
        N = (R & 128) != 0;
        S = N ^ V;
        Z = R == 0;

        registers.put(Rd, R);
    }

    private void LDS(String Rg, int address) {
        // Ignores flag byte
        PC += 2;
        byte value = memory.get(address);
        registers.put(Rg, value);
    }

    private void ADD(String Rd, String Rr) {
        PC += 1;
        byte rd = registers.get(Rd);
        byte rr = registers.get(Rr);

        byte R = (byte) (rd + rr);

        H = ((rd & rr | rr & ~R | ~R & rd) & 8) != 0;
        V = ((rd & rr & ~R | ~rd & ~rr & R) & 128) != 0;
        N = (R & 128) != 0;
        S = N ^ V;
        Z = R == 0;
        C = ((rd & rr | rr & ~R | ~R & rd) & 128) != 0;

        registers.put(Rd, R);
    }

    private void ROL(String Rd) {
        PC += 1;
        int rd = registers.get(Rd) << 1;
        rd |= C? 1:0;

        C = (rd & 256) != 0;
        H = (rd & 8) != 0;
        N = (rd & 128) != 0;
        V = N ^ C;
        Z = rd == 0;
        S = N ^ V;


        registers.put(Rd, (byte) rd);
    }

    private void LSR(String Rd) {
        PC += 1;
        int rd = registers.get(Rd) & 255;
        System.out.println("RD = " + rd);
        C = (rd & 1) != 0;
        rd >>= 1;
        N = false;
        V = N ^ C;
        Z = rd == 0;
        S = N ^ V;


        registers.put(Rd, (byte) rd);
    }

    private void MUL(String Rd, String Rr) {
        PC++;
        int rr = registers.get(Rr) & 0xFF;
        int rd = registers.get(Rd) & 0xFF;
        int R = rr * rd;
        int r0 = R & 255;
        int r1 = R >> 8;
        C = R >> 15 != 0;
        registers.put("R1", (byte) r1);
        registers.put("R0", (byte) r0);
    }

    private void AND(String Rd, String Rr) {
        PC++;
        int rd = registers.get(Rd);
        int rr = registers.get(Rr);

        int r = rd & rr;
        Z = r == 0;
        N = (r & 128) != 0;
        V = false;
        S = N ^ V;
        registers.put(Rd, (byte) r);
    }

    private void ICALL() {
        STACK = PC + 1;
        PC = subprogram.addr;
        SP -= 2;
        memory.put(0x25F, (byte) (STACK >> 8));
        memory.put(0x25E, (byte) (STACK & 255));
        System.out.println(this);

        exec(subprogram.body);
    }

    private void RET() {
        PC = STACK;
    }

    private void SUB(String Rd, String Rr) {
        PC += 1;
        byte rd = registers.get(Rd);
        byte rr = registers.get(Rr);

        final byte R = (byte) (rd - rr);

        H = ((rd & rr | rr & R | R & rd) & 8) != 0;
        V = ((rd & ~rr & R | ~rd & rr & R) & 128) != 0;
        N = (R & 128) != 0;
        S = N ^ V;
        Z = R == 0;
        C = ((~rd & ~rr | ~rr & ~R | ~R & ~rd) & 128) != 0;

        registers.put(Rd, R);
    }

    private void SWAP(String Rd) {
        PC++;
        int rd = registers.get(Rd) & 255;
        int greater = rd >> 4;
        int lesser = rd & 0b1111;
        int R = (lesser << 4) + greater;
        registers.put(Rd, (byte) R);
    }

    private void SBC(String Rd, String Rr) {
        PC += 1;
        int rd = registers.get(Rd);
        int rr = registers.get(Rr);
        int R = rd - rr - (C?1:0);

        H = ((~rd & rr | rr & R | R & ~rd) & 8) != 0;
        V = ((rd & ~rr & ~R | ~rd & rr & R) & 128) != 0;
        N = (R & 128) != 0;
        S = N ^ V;
        Z = R == 0 && Z;
        C = ((~rd & rr | rr & R | R & ~rd) & 128) != 0;

        registers.put(Rd, (byte) R);
    }

    private void NEG(String Rd) {
        PC += 1;
        byte rd = registers.get(Rd);
        int R = ~rd;
        H = ((R & ~rd) & 8) != 0;
        V = R == 0x80;
        N = (R & 128) != 0;
        S = N ^ V;
        Z = R == 0;
        C = R != 0x80;
        registers.put(Rd, (byte) R);
    }

    public String newString() {
        String result = String.format("|\tPC = %s\t|\tPC = %s\t|\n", hex(backup.PC), hex(PC));
        switch (lastCommand) {
            case "ICALL":
            case "RET":
                result += getStackInfo();
                break;
            default:
                result += getRegsInfo();
        }
        return result;
    }

    private String getRegsInfo() {
        StringBuilder result = new StringBuilder();
        for (String reg : registers.keySet()) {
            String val = registers.containsKey(reg)? hex(registers.get(reg)) : "XX";
            String backupVal = backup.registers.containsKey(reg)? hex(backup.registers.get(reg)) : "XX";
            result.append(String.format("|\t%s = %s\t|\t%s = %s\t|\n", reg, backupVal, reg, val));
        }
        return result.toString();
    }

    private String getStackInfo() {
        StringBuilder result = new StringBuilder();
        result.append(String.format("|\t$25E = %s\t|\t$25E = %s\t|\n", backup.memory.get(0x25E), memory.get(0x25E)));
        result.append(String.format("|\t$25F = %s\t|\t$25F = %s\t|\n", backup.memory.get(0x25F), memory.get(0x25F)));
        result.append(String.format("|\tSP = %s\t|\tSP = %s\t|\n", hex(backup.SP), hex(SP)));

        return result.toString();
    }

    @Override
    public String toString() {
        return newString();
        /*
        StringBuilder regs = new StringBuilder("REGS {");
        for (String register : registers.keySet()) {
            regs.append(register);
            regs.append(" = ");
            regs.append('$').append(hex(registers.get(register)));
            regs.append("; ");
        }
        regs.append("}");

        StringBuilder mem = new StringBuilder("MEMORY {");
        for (int memEntry : memory.keySet()) {
            mem.append(hex(memEntry));
            mem.append(" = ");
            mem.append('$').append(hex(memory.get(memEntry)));
            mem.append("; ");
        }
        mem.append("}");
        return "PC = " + hex(PC) + "; SP = " + hex(SP) + "; STACK = " + hex(STACK) + "\n" + regs + " " + mem + "\n" + getSREG();
         */
    }

    static String hex(int value) {
        return Integer.toString(value & 0xFFFF, 16).toUpperCase();
    }

    static String hex(byte value) {
        return Integer.toString(value & 0xFF, 16).toUpperCase();
    }

    String getSREG() {
        String result = "";

        result += "|\tI\t|\tT\t|\tH\t|\tS\t|\tV\t|\tN\t|\tZ\t|\tC\t|";
        result += "\n|\t" + (I? 1:0) + "\t" +
                "|\t" + (T? 1:0) + "\t" +
                "|\t" + (H? 1:0) + "\t" +
                "|\t" + (S? 1:0) + "\t" +
                "|\t" + (V? 1:0) + "\t" +
                "|\t" + (N? 1:0) + "\t" +
                "|\t" + (Z? 1:0) + "\t" +
                "|\t" + (C? 1:0) + "\t|";

        return result;
    }
}
