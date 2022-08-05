package com.wrmsr.jwam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Warren's Abstract Machine
 *
 * @author Bruno Simões e Pedro Guerreiro
 * @version 1.00
 */
public class Wam extends Symbol {

    /**
     * Contêm os registos da máquina WAM.
     */
    private Registers registers;

    /**
     * Divisão abstracta da zona de memória para o código.
     */
    protected Code code;

    /**
     * Divisão abstracta da zona de memória para a Heap.
     */
    protected Heap heap;

    /**
     * Divisão abstracta da zona de memória para a Stack.
     */
    protected LocalStack stack;

    /**
     * Divisão abstracta da zona de memória para o Trail.
     */
    protected Trail trail;

    /**
     * Divisão abstracta da zona de memória para a PDL.
     */
    protected PDL pdl;

    /**
     * Nivel de debug.
     */
    protected int debug = 0;

    /**
     * Abstração sobre o conteúdo de uma celula independentemente
     * do seu tipo ou localização.
     */

    protected Store store;

    /**
     * Variavel que nos indica quando se produziu um fail e tem que se fazer
     * backtrack.
     */

    private boolean fail;

    /**
     * Variavel que nos indica em que modo se encontra a máquina (este modo
     * pode ser de leitura ou de escrita)
     */

    private byte mode;

    /**
     * 'Registo' com o número de argumentos.
     */
    private int numOfArgs;

    /**
     * Representação virtual da memoria da wam
     */
    private DataCell[] memory;

    /**
     * Aloca a memoria em kb.
     */
    public String malloc(int memorySize, int heapSize) {
        int codeSize, stackSize, trailSize, pdlSize;

        if (heapSize != -1) {
            stackSize = trailSize = memorySize / 256 * 84;
            pdlSize = memorySize / 256 * 32;
            codeSize = heapSize;
        } else {
            heapSize = memorySize / 256 * 56;
            stackSize = memorySize / 256 * 64;
            trailSize = memorySize / 256 * 64;
            pdlSize = memorySize / 256 * 16;
            codeSize = memorySize - heapSize - stackSize - trailSize - pdlSize;
        }

        if (heapSize > 265 * 1024) {
            return "ERROR: decrease heap size";
        }

        /** 10K é um bom minimo */
        else if (heapSize < 10) {
            return "ERROR: increase heap size";
        }

        /** multiplica por 1K */
        codeSize <<= 10;
        heapSize <<= 10;
        stackSize <<= 10;
        trailSize <<= 10;
        pdlSize <<= 10;
        memorySize <<= 10;

        memory = new DataCell[memorySize];
        clear();

        try {
            code = new Code(codeSize - 1);
            heap = new Heap(codeSize, codeSize + heapSize - 1);
            stack = new LocalStack(codeSize + heapSize, codeSize + heapSize + stackSize - 1);
            trail = new Trail(codeSize + heapSize + stackSize);
            pdl = new PDL(memorySize - 1);
            store = new Store();
        } catch (MemoryOverflow e) {
            return e.getMessage();
        }

        return "\nMemory Allocation of " + (memorySize >>= 10) + " kb\n" + "Code " + (codeSize >>= 10) + " kb\n" + "Heap " + (heapSize >>= 10) + " kb\n" + "Stack " + (stackSize >>= 10) + " kb\n" + "Trail " + (trailSize >>= 10) + " kb\n" + "PDL " + (pdlSize >>= 10) + " kb\n";
    }

    /**
     * Efectua um reset a memória.
     */
    public void clear() {
        if (memory != null) {
            for (int i = 0; i < memory.length; i++) {
                memory[i] = null;
            }
        }
    }

    /**
     * Define o nivel de debug.
     *
     * @param i nivel de debug.
     */

    public void setDebug(int i) {
        debug = i;
    }

    /**
     * Constructor
     */
    public Wam() {
        init();
    }

    /**
     * Inicializa a WAM
     */
    public void init() {
        mode = 0;
        fail = false;
        numOfArgs = 0;
        registers = new Registers();
        write("" + malloc(512, 15), 1);
    }

    /**
     * Um <i>choice point frame</i> é alocado na stack da seguinte forma.<br>
     * <br>
     * B				n - Quantidade de argumentos.<br>
     * B + 1			A1 - Registo do argumento 1.<br>
     * B + n			An - Registo do argumento n.<br>
     * B + n + 1		CE - continuation environment
     * B + n + 2		CP - continuation pointer
     * B + n + 3		B - previous choice point
     * B + n + 4		BP - next clause
     * B + n + 5		TR - trail pointer
     * B + n + 6		H - heap pointer
     * B + n + 7		B0 - cut pointer
     */

    public void updateChoisePoint() throws Exception {
        int B = registers.getB();
        int n = 0;
        memory[B] = new DataCell(n);

        for (int i = 0; i < n; i++) {
            memory[B + i + 1] = registers.getRegister("A" + i);
        }

        memory[B + n + 1] = new DataCell(registers.getCE());
        memory[B + n + 2] = new DataCell(registers.getCP());
        memory[B + n + 3] = new DataCell(B);
        memory[B + n + 4] = new DataCell(code.getAddrNextInstruction());
        memory[B + n + 5] = new DataCell(registers.getTR());
        memory[B + n + 6] = new DataCell(registers.getH());
        memory[B + n + 7] = new DataCell(registers.getB0());
    }

    /**
     * 'Environment frame' é alocado na stack da seguinte forma:<br>
     * <br>
     * E				CE continuation environment<br>
     * E + 1			CP continuation code<br>
     * E + 2			Y1 - 1st local variable<br>
     * E + n + 1		Yn - nth local variable<br>
     */

    public void updateEnvironment() throws Exception {
        int E = registers.getE();
        int n = 0;
        /** Aponta para Halt. */
        int CP = registers.getCP();
        memory[E] = new DataCell(E);
        memory[E + 1] = new DataCell(CP);
        for (int i = 1; i <= n; i++) {
            memory[E + i + 1] = registers.getRegister("Y" + (i - 1));
        }
        registers.setB(E + n + 2);
        registers.setB0(E + n + 2);
    }

    /**
     * Executa a instrução para a qual o registo P aponta.
     *
     * @return true se o programa deve continuar ou false se tem que parar.
     */

    public boolean doInstruction() {

        int P = registers.getP();
        while (P >= 0 && !fail) {
            Instruction s;
            try {
                s = code.getInstruction();

                try {
                    if (debug == 3) {
                        getMemInformation();
                        new BufferedReader(new InputStreamReader(System.in)).readLine();
                    }
                } catch (IOException io) {
                }

                write("\n\nExecuting instruction at 0x" + P + "", 1);
                write(" [" + s.toString() + "]\n", 1);

                switch ((Integer) keys.get(s.getOp())) {
                    case CUT:
                        proceed();
                        break;
                    case GET_LEVEL:
                        proceed();
                        break;
                    case NECK_CUT:
                        proceed();
                        break;
                    case SWITCH_ON_STRUCTURE:
                        proceed();
                        break;
                    case SWITCH_ON_CONSTANT:
                        proceed();
                        break;
                    case SWITCH_ON_TERM:
                        proceed();
                        break;

                    case TRUST:
                        trust(s.getArg());
                        break;
                    case RETRY:
                        retry(s.getArg());
                        break;
                    case TRY:
                        t_r_y(s.getArg());
                        break;
                    case TRUST_ME:
                        trustMe();
                        break;
                    case RETRY_ME_ELSE:
                        retryMeElse(s.getArg());
                        break;
                    case TRY_ME_ELSE:
                        tryMeElse(s.getArg());
                        break;
                    case PROCEED:
                        proceed();
                        break;
                    case EXECUTE:
                        execute(s.getArg());
                        break;
                    case CALL:
                        call(s.getArg());
                        break;
                    case DEALLOCATE:
                        deallocate();
                        break;
                    case ALLOCATE:
                        allocate();
                        break;
                    case UNIFY_VOID:
                        unifyVoid(s.getArg());
                        break;
                    case UNIFY_CONSTANT:
                        unifyConstant(s.getArg());
                        break;
                    case UNIFY_LOCAL_VALUE:
                        unifyLocalValue(s.getArg());
                        break;
                    case UNIFY_VALUE:
                        unifyValue(s.getArg());
                        break;

                    case UNIFY_VARIABLE:
                        proceed();
                        break;

                    case SET_VOID:
                        setVoid(s.getArg());
                        break;
                    case SET_CONSTANT:
                        setConstant(s.getArg());
                        break;
                    case SET_LOCAL_VALUE:
                        setLocalValue(s.getArg());
                        break;
                    case SET_VALUE:
                        setValue(s.getArg());
                        break;
                    case SET_VARIABLE:
                        setVariable(s.getArg());
                        break;
                    case GET_CONSTANT:
                        getConstant(s.getArg(), s.getArg());
                        break;
                    case GET_LIST:
                        getList(s.getArg());
                        break;
                    case GET_STRUCTURE:
                        getStructure(s.getArg(), s.getArg());
                        break;
                    case GET_VALUE:
                        getValue(s.getArg(), s.getArg());
                        break;
                    case GET_VARIABLE:
                        getVariable(s.getArg(), s.getArg());
                        break;
                    case PUT_CONSTANT:
                        putConstant(s.getArg(), s.getArg());
                        break;
                    case PUT_LIST:
                        putList(s.getArg());
                        break;
                    case PUT_STRUCTURE:
                        putStructure(s.getArg(), s.getArg());
                        break;
                    case PUT_UNSAFE_VALUE:
                        putUnsafeValue(s.getArg(), s.getArg());
                        break;
                    case PUT_VALUE:
                        putValue(s.getArg(), s.getArg());
                        break;
                    case PUT_VARIABLE:
                        putVariable(s.getArg(), s.getArg());
                        break;
                    case HALT:
                        return false;

                    default: {
                        write("Invalid operation in line ", 1);
                        backtrack();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * Cria o CP e E inicial.
     * Executa as instruções.
     */

    public boolean run() {
        try {
            updateEnvironment();
            updateChoisePoint();
        } catch (Exception e) {
            write(e.getMessage(), 0);
        }

        //  boolean t = true;
        // while (t){
        //    t = doInstruction();
        doInstruction();
        //   if (fail){
        //      t = backtrack();
        //      write("WAM: " + fail, 0);
        //     fail = false;
        //    if (!t)
        //        write("FAIL", 0);
        // }
        // }
        write(registers.toString(), 1);
        write("\n", 0);
        return fail;
    }

    /**
     * Instrução put_variable Vn, Ai.<br><br>
     * <p/>
     * if(v == Yn) putVariableStack(Vn, Ai)<br>
     * if(v == Xn) putVariableHeap(Vn, Ai)<br>
     *
     * @param v registo Vn
     * @param a registo Ai
     */

    public void putVariable(String v, String a) throws Exception {
        switch (v.charAt(0)) {
            case 'Y':
                putVariableStack(v, a);
                break;
            case 'X':
                putVariableHeap(v, a);
                break;
            default:
                write("public void putVariable(" + v + ", " + a + ")", 0);
        }
    }

    /**
     * Instrução put_variable Xn, Ai.<br><br>
     * <p/>
     * Cria uma nova célula REF na heap e copia-a para os registos Xn e Ai.<br>
     * Continua a execução com a instrução seguinte.
     */

    public void putVariableHeap(String Xn, String Ai) throws Exception {
        int H = registers.getH();
        heap.setAddress(H, H);
        registers.setRegister(Xn, heap.getAddress(H));
        registers.setRegister(Ai, heap.getAddress(H));
        registers.setH(H + 1);
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução put_variable Yn, Ai.<br><br>
     * <p/>
     * Inicializa a enésima variável na stack, no actual ambiente para ‘unbound’ e
     * coloca o Ai a apontar para ela.<br>
     * Continua a execução com a instrução seguinte.<br><br>
     * <p/>
     * E + n + 1 representa a enésima variável na stack.<br>
     */

    public void putVariableStack(String Yn, String Ai) throws Exception {
        int n = Util.getRegisterIndex(Yn) + 1;
        int addr = registers.getE() + n + 1;
        stack.setAddress(addr, addr);
        registers.setRegister(Ai, stack.getAddress(addr));
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução put_value Vn, Ai.<br><br>
     * <p/>
     * Coloca o conteúdo de Vn no registo Ai.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void putValue(String Vn, String Ai) throws Exception {
        registers.setRegister(Ai, registers.getRegister(Vn));
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução put_unsafe_value Yn, Ai.<br><br>
     * <p/>
     * Se o valor de-referenciado de Yn não é uma variável unbound da
     * stack no actual environment define Ai a apontar para esse valor.<br>
     * Noutros casos efectua um bind à variável da stack referenciada
     * para uma nova célula unbound adicionada na heap e define Ai a
     * apontar para essa célula.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void putUnsafeValue(String Yn, String Ai) throws Exception {
        int E = registers.getE(), H = registers.getH(), n = Util.getRegisterIndex(Yn) + 1;
        int addr = deref(E + n + 1);
        if (addr < E) {
            registers.setRegister(Ai, store.getRegister(addr));
        } else {
            heap.setAddress(H, H);
            bind(addr, H);
            registers.setRegister(Ai, heap.getAddress(H));
            registers.setH(H + 1);
        }
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução put_structure f/n, Ai.<br><br>
     * <p/>
     * Coloca uma nova célula contendo um functor na heap e
     * define o registo Ai como um célula STR a apontar para a célula do functor.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void putStructure(String f, String Ai) throws Exception {
        int H = registers.getH();
        heap.setAddress(H, new DataCell(FUN, f, H));
        registers.setRegister(Ai, new DataCell(STR, H));
        registers.setH(H + 1);
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução put_list Ai.<br><br>
     * <p/>
     * Define o registo Ai com uma célula LIS a apontar para o actual topo da heap.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void putList(String Ai) throws Exception {
        int H = registers.getH();
        registers.setRegister(Ai, new DataCell(LIS, H));
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução put_constant c, Ai.<br><br>
     * <p/>
     * Coloca uma célula contendo uma constante c no registo Ai.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void putConstant(String c, String Ai) throws Exception {
        registers.setRegister(Ai, new DataCell(CON, c, Util.getRegisterIndex(Ai)));
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução get_variable Vn, Ai.<br><br>
     * <p/>
     * Coloca o conteúdo do registo Ai na variável Vn.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void getVariable(String Vn, String Ai) throws Exception {
        registers.setRegister(Vn, registers.getRegister(Ai));
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução get_value Vn, Ai.<br><br>
     * <p/>
     * Unifica a variável Vn e o registo Ai.<br>
     * Faz backtrack se falhar ou então continua.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void getValue(String Vn, String Ai) throws Exception {
        if (!unify(registers.getRegister(Vn).getValue(), registers.getRegister(Ai).getValue())) {
            registers.setP(code.getAddrNextInstruction());
        } else {
            backtrack();
        }
    }

    /**
     * Instrução get_structure f/n, Ai.<br><br>
     * <p/>
     * Se o valor de-referenciado do registo Ai é uma variável unbound,
     * então efectua bind da nova célula REF a apontar para o functor f
     * colocado na heap e define o mode como WRITE; Caso seja uma célula STR
     * a apontar para o functor f, então define o registo S como o endereço
     * que sucede o endereço na heap do functor f e define o mode como READ. <br>
     * Se não é uma célula STR ou o functor é diferente de f, falha.<br>
     * Faz backtrack se falhar ou então continua a execução com a instrução seguinte.<br>
     */

    public void getStructure(String f, String Ai) throws Exception {
        boolean fail = false;
        int H = registers.getH();
        registers.setS(1);
        int addr = deref(registers.getRegister(Ai).getValue());
        if (store.getRegister(addr).equals(REF)) {
            heap.setAddress(H, new DataCell(STR, H + 1));
            heap.setAddress(H + 1, new DataCell(FUN, f, H + 1));
            bind(addr, H);
            registers.setH(H + 2);
            mode = WRITE;
        } else if (store.getRegister(addr).equals(STR)) {
            int a = store.getRegister(addr).getValue();
            if (heap.getAddress(a).compareFunctor(f)) {
                registers.setS(a + 1);
                mode = READ;
            } else {
                fail = true;
            }
        } else {
            fail = true;
        }

        if (fail) {
            backtrack();
        } else {
            registers.setP(code.getAddrNextInstruction());
        }
    }

    /**
     * Instrução get_list Ai.<br><br>
     * <p/>
     * Se o valor de-referenciado do registo Ai é uma variável unbound,
     * então efectua bind da nova célula REF colocada no topo da heap
     * e define o mode como WRITE; Caso seja uma célula LIS define o registo
     * S com o endereço que ela contem e define o mode como READ. <br>
     * Se não é uma célula LIS, falha.<br>
     * Faz backtrack se falhar ou então continua a execução com a instrução seguinte.<br>
     */

    public void getList(String Ai) throws Exception {
        int H = registers.getH();
        int addr = deref(registers.getRegister(Ai).getValue());
        if (store.getRegister(addr).equals(REF)) {
            heap.setAddress(H, new DataCell(LIS, H + 1));
            bind(addr, H);
            registers.setH(H + 1);
            mode = WRITE;
        } else if (store.getRegister(addr).equals(LIS)) {
            int a = store.getRegister(addr).getValue();
            registers.setS(a);
            mode = READ;
        } else {
            fail = true;
        }

        if (fail) {
            backtrack();
        } else {
            registers.setP(code.getAddrNextInstruction());
        }
    }

    /**
     * Instrução get_constant c, Ai.<br><br>
     * <p/>
     * Se o valor de-referenciado do registo Ai é uma
     * variável unbound, então faz bind dessa variável para a constante c. <br>
     * Noutros casos falha se não for a constante c.<br>
     * Faz backtrack se falhar ou então continua a execução com a instrução seguinte.<br>
     */

    public void getConstant(String c, String Ai) throws Exception {

        int addr = deref(registers.getRegister(Ai).getValue());
        if (store.getRegister(addr).equals(REF)) {
            //	store.setAddress(addr, new DataCell(CON, c, addr));

            DataCell d = registers.getRegister(Ai);
            d.setStringValue(c);
            store.setAddress(addr, d);

            trail(addr);
        } else if (store.getRegister(addr).equals(CON)) {
            store.getRegister(addr).setStringValue(c);
            //fail = !store.getRegister(addr).getStringValue().equals(c);
        } else {
            fail = true;
        }

        if (fail) {
            backtrack();
        } else {
            registers.setP(code.getAddrNextInstruction());
        }
    }

    /**
     * Instrução set_variable Vn.<br><br>
     * <p/>
     * Coloca uma nova célula REF na heap e copia-a para a variável Yn.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void setVariable(String Vn) throws Exception {
        int H = registers.getH();
        heap.setAddress(H, H);
        registers.setRegister(Vn, heap.getAddress(H));
        registers.setH(H + 1);
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução set_value Vn.<br><br>
     * <p/>
     * Coloca o valor de Vn na heap.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void setValue(String Vn) throws Exception {
        int H = registers.getH();
        heap.setAddress(H, registers.getRegister(Vn));
        registers.setH(H + 1);
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução set_local_value Vn.<br><br>
     * <p/>
     * Se o valor de-referenciado de Vn é uma variável
     * unbound da heap, coloca uma cópia na heap.<br>
     * Se o valor de-referenciado de Vn é uma variável
     * unbound da stack coloca uma nova célula REF unbound
     * na heap e faz bind à variável da stack para essa. <br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void setLocalValue(String Vn) throws Exception {
        int H = registers.getH();
        int addr = deref(registers.getRegister(Vn).getValue());
        if (addr < H) {
            heap.setAddress(H, heap.getAddress(addr));
        } else {
            heap.setAddress(H, H);
            bind(addr, H);
        }
        registers.setH(H + 1);
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução set_constant c.<br><br>
     * <p/>
     * Coloca uma célula com uma constante c na heap.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void setConstant(String c) throws Exception {
        int H = registers.getH();
        heap.setAddress(H, new DataCell(CON, c, H));
        registers.setH(H + 1);
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução set_void n.<br><br>
     * <p/>
     * Coloca n células REF – unbound na heap.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void setVoid(String v) throws Exception {
        int n = Integer.parseInt(v);
        int H = registers.getH();
        for (int i = H; i <= H + n - 1; i++) {
            heap.setAddress(i, i);
        }
        registers.setH(H + n);
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução unify_value Vn.<br><br>
     * <p/>
     * No modo READ, unifica a variável An e o
     * endereço na heap S; No modo de escrita,
     * coloca o valor Vn na heap. <br>
     * Tanto no modo READ como WRITE, S é incrementado por 1.<br>
     * Faz backtrack se falhar ou continua a execução com a próxima instrução.<br>
     */

    public void unifyValue(String Vn) throws Exception {
        boolean fail = false;
        int S = registers.getS();
        int H = registers.getH();

        if (mode == READ) {
            fail = unify(registers.getRegister(Vn).getValue(), S);
        } else {
            heap.setAddress(H, registers.getRegister(Vn));
            registers.setH(H + 1);
        }
        registers.setS(S + 1);
        if (fail) {
            backtrack();
        } else {
            registers.setP(code.getAddrNextInstruction());
        }
    }

    /**
     * Instrução unify_local_value Vn.<br><br>
     * <p/>
     * No modo READ, unifica a variável An e o endereço na
     * heap S; No modo de WRITE, se o valor de-referenciado
     * é uma variável unbound na heap, coloca uma cópia na heap. <br>
     * Se for um endereço unbound na stack, coloca uma nova célula
     * REF na heap e faz um bind à variável na stack para essa.<br>
     * Tanto no modo READ como WRITE, S é incrementado por 1.<br>
     * Faz backtrack se falhar ou continua a execução com a próxima instrução.<br>
     */

    public void unifyLocalValue(String Vn) throws Exception {
        boolean fail = false;
        int S = registers.getS();
        if (mode == READ) {
            fail = unify(registers.getRegister(Vn).getValue(), registers.getS());
        } else {
            int H = registers.getH();
            int addr = deref(registers.getRegister(Vn).getValue());
            if (addr < H) {
                heap.setAddress(H, heap.getAddress(addr));
            } else {
                heap.setAddress(H, H);
                bind(addr, H);
            }
            registers.setH(H + 1);
        }
        registers.setS(S + 1);
        if (fail) {
            backtrack();
        } else {
            registers.setP(code.getAddrNextInstruction());
        }
    }

    /**
     * Instrução unify_constant c.<br><br>
     * <p/>
     * Em modo de leitura de-referencia o endereço S na heap.<br>
     * Se o resultado for um variável unbound,
     * faz bind a essa variável para uma constante c;
     * Noutros casos, falha se o resultado é diferente da constante c.<br>
     * No modo de escrita, coloca uma célula com a constante c na heap.<br>
     * Faz backtrack se falhar ou continua com a próxima instrução.<br>
     */

    public void unifyConstant(String c) throws Exception {
        int H = registers.getH();
        boolean fail = false;
        if (mode == READ) {
            int addr = deref(registers.getS());
            DataCell dc = store.getAddress(addr);
            if (dc.equals(REF)) {
                store.setAddress(addr, new DataCell(CON, c, addr));
                trail(addr);
            } else if (dc.equals(CON)) {
                fail = !dc.getStringValue().equals(c);
            } else {
                fail = true;
            }
        } else {
            heap.setAddress(H, new DataCell(CON, c, H));
            registers.setH(H + 1);
        }
        if (fail) {
            backtrack();
        } else {
            registers.setP(code.getAddrNextInstruction());
        }
    }

    /**
     * Instrução unify_void n.<br><br>
     * <p/>
     * No modo de escrita, coloca N células REF unbound na heap.<br>
     * No modo de leitura, salta as próximas N células na heap começando no endereço S.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void unifyVoid(String v) throws Exception {
        int n = Util.arity(v), S = registers.getS(), H = registers.getH();

        if (mode == READ) {
            registers.setS(S + n);
        } else {
            for (int i = H; i <= H + n - 1; i++) {
                heap.setAddress(i, i);
            }
            registers.setH(H + n);
        }
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução try_me_else L.<br><br>
     * <p/>
     * Aloca um novo choice point frame na stack, definindo a sua
     * próxima cláusula como <i>L</i>, os outros campos de acordo
     * com o actual contexto, e apontando B para ele.<br>
     * Continua a execução com a instrução seguinte.<br><br>
     */

    public void tryMeElse(String v) throws Exception {
        //int L = Util.arity(v),
        int E = registers.getE(), B = registers.getB(), CP = registers.getCP(),
                //	B0 = registers.getB0(),
                TR = registers.getTR(), H = registers.getH(), newB, n;

        if (E > B) {
            n = Util.arity(code.getAddress(stack.getValue(E + 1) - 1).getStringValue());

            newB = E + n + 2;
        } else {
            newB = B + stack.getValue(B) + 8;
        }

        stack.setAddress(newB, numOfArgs);
        n = stack.getAddress(newB).getValue();
        for (int i = 0; i < n; i++) {
            stack.setAddress(newB + i + 1, registers.getRegister("A" + i));
        }

        stack.setAddress(newB + n + 1, E);
        stack.setAddress(newB + n + 2, CP);
        stack.setAddress(newB + n + 3, B);
        stack.setAddress(newB + n + 4, code.getInstrAddress(v));
        stack.setAddress(newB + n + 5, TR);
        stack.setAddress(newB + n + 6, H);

        registers.setB(newB);
        registers.setHB(stack.getAddress(B + stack.getAddress(B).getValue() + 6).getValue());
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução retry_me_else L.<br><br>
     * <p/>
     * Tendo acontecido backtrack para o actual choise point,
     * faz reset a toda a informação necessária e actualiza o
     * campo da próxima cláusula para L.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void retryMeElse(String v) throws Exception {
        //int L = Util.arity(v),
        int B = registers.getB(), n = stack.getAddress(B).getValue(), TR = registers.getTR();

        /** for i <- 1 to n do Ai <- STACK[B + i]; */
        for (int i = 0; i < n; i++) {
            registers.setRegister("A" + i, stack.getAddress(B + i + 1));
        }

        registers.setE(stack.getAddress(B + n + 1).getValue());
        registers.setCP(stack.getAddress(B + n + 2).getValue());
        stack.setAddress(B + n + 4, code.getInstrAddress(v));
        unWindTrail(stack.getAddress(B + n + 5).getValue(), TR);
        registers.setTR(stack.getAddress(B + n + 5).getValue());
        registers.setH(stack.getAddress(B + n + 6).getValue());
        registers.setHB(stack.getValue(B + stack.getValue(B) + 6));
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução trust_me.<br><br>
     * <p/>
     * Tendo acontecido backtrack para o actual choise point,
     * faz reset a toda a informação necessária dele e faz reset
     * ao B para o seu predecessor.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void trustMe() throws Exception {
        int B = registers.getB(), n = stack.getValue(B), TR = registers.getTR();

        for (int i = 0; i < n; i++) {
            registers.setRegister("A" + i, stack.getAddress(B + i + 1));
        }

        registers.setE(stack.getValue(B + n + 1));
        registers.setCP(stack.getValue(B + n + 2));
        stack.setAddress(B + n + 4, code.getAddrNextInstruction());
        unWindTrail(stack.getValue(B + n + 5), TR);
        registers.setTR(stack.getValue(B + n + 5));
        registers.setH(stack.getValue(B + n + 6));
        registers.setB(stack.getValue(B + n + 3));
        registers.setHB(stack.getValue(B + stack.getValue(B) + 6));
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução try L.<br><br>
     * <p/>
     * Aloca um novo choise point frame na stack,
     * definindo a sua próxima clausula para a próxima
     * instrução e os outros campos de acordo com o actual contexto.
     * Coloca o B a apontar para ele.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void t_r_y(String v) throws Exception {
        int L = Util.arity(v), E = registers.getE(), B = registers.getB(), CP = registers.getCP(), B0 = registers.getB0(), TR = registers.getTR(), H = registers.getH(), newB, n;

        if (E > B) {
            n = Util.arity(code.getAddress(stack.getAddress(E + 1).getValue() - 1).getStringValue());

            newB = B + n + 8;
        } else {
            newB = B + stack.getAddress(B).getValue() + 8;
        }

        stack.setAddress(newB, this.numOfArgs);
        n = stack.getAddress(newB).getValue();

        /** for i <- 1 to n do STACK[newB + i] <- Ai */
        for (int i = 0; i < n; i++) {
            stack.setAddress(newB + i + 1, registers.getRegister("A" + i));
        }

        stack.setAddress(newB + n + 1, E);
        stack.setAddress(newB + n + 2, CP);
        stack.setAddress(newB + n + 3, B);
        stack.setAddress(newB + n + 4, code.getAddrNextInstruction());
        stack.setAddress(newB + n + 5, TR);
        stack.setAddress(newB + n + 6, H);
        stack.setAddress(newB + n + 7, B0);

        registers.setB(newB);
        registers.setHB(H);
        registers.setP(L);
    }

    /**
     * Instrução retry L.<br><br>
     * <p/>
     * Tendo acontecido backtrack para o actual choise point,
     * faz reset a toda a informação necessária dele e actualiza
     * o campo da próxima cláusula para a próxima instrução.<br>
     * Continua a execução com a instrução de label L.<br>
     */

    public void retry(String v) throws Exception {
        int B = registers.getB(), n = stack.getAddress(B).getValue(), L = Util.arity(v), TR = registers.getTR();

        /** for i <- 0 to n - 1 do Ai <- STACK[B + i]; */
        for (int i = 0; i <= n - 1; i++) {
            registers.setRegister("A" + i, stack.getAddress(B + i + 1));
        }

        registers.setE(stack.getAddress(B + n + 1).getValue());
        registers.setCP(stack.getAddress(B + n + 2).getValue());
        stack.setAddress(B + n + 4, code.getAddrNextInstruction());
        unWindTrail(stack.getAddress(B + n + 5).getValue(), TR);
        registers.setTR(stack.getAddress(B + n + 5).getValue());
        registers.setH(stack.getAddress(B + n + 6).getValue());
        registers.setHB(registers.getH());
        registers.setP(L);
    }

    /**
     * Instrução trust L.<br><br>
     * <p/>
     * Tendo acontecido backtrack para o actual choise point,
     * faz reset a toda a informação necessária dele e faz reset
     * ao B para o seu predecessor.<br>
     * Continua a execução com a instrução de label L.<br>
     */

    public void trust(String v) throws Exception {
        int B = registers.getB(), n = stack.getAddress(B).getValue(), L = Util.arity(v), TR = registers.getTR();

        /** for i <- 1 to n do Ai <- STACK[B + i]; */
        for (int i = 0; i < n; i++) {
            registers.setRegister("A" + i, stack.getAddress(B + i));
        }

        registers.setE(stack.getAddress(B + n + 1).getValue());
        registers.setCP(stack.getAddress(B + n + 2).getValue());
        unWindTrail(stack.getAddress(B + n + 5).getValue(), TR);
        registers.setTR(stack.getAddress(B + n + 5).getValue());
        registers.setH(stack.getAddress(B + n + 6).getValue());
        registers.setHB(stack.getAddress(B + n + 3).getValue());
        registers.setP(L);
    }

    /**
     * Instrução allocate.<br><br>
     * <p/>
     * Aloca um novo environment na stack,
     * define o cont. env. e o cont. point. para o
     * actual E e CP respectivamente.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void allocate() throws Exception {
        int E, B, CP, n, newE;
        E = registers.getE();
        B = registers.getB();
        CP = registers.getCP();

        if (E > B) {
            n = Util.arity(code.getAddress(stack.getValue(E + 1) - 1).getStringValue());
            newE = E + n + 2;
        } else {
            newE = B + stack.getAddress(B).getValue() + 8;
        }

        stack.setAddress(newE, new DataCell(E));
        stack.setAddress(newE + 1, new DataCell(CP));
        registers.setE(newE);
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução deallocate.<br><br>
     * <p/>
     * Remove o <i>environment frame</i> da stack na localização
     * E fazendo reset ao valor de E para o valor de seu
     * campo CE e o <i>continuation pointer</i> (CP) para o valor
     * do seu CP.<br>
     * Continua a execução com a instrução seguinte.<br>
     */

    public void deallocate() throws Exception {
        write("\n[DEALLOCATE]\n" + registers.toString() + "\n", 1);
        int E = registers.getE();
        registers.setCP(stack.getAddress(E + 1).getValue());
        registers.setE(stack.getAddress(E).getValue());
        registers.setP(code.getAddrNextInstruction());
    }

    /**
     * Instrução call p/n.<br><br>
     * <p/>
     * Se o P está definido, então guarda o <i>choise point’s address</i>
     * actual em B0 e o valor do actual continuation pointer em CP e
     * continua a execução com a instrução de label P, com as variáveis
     * da stack a permanecer no actual env. <br>
     * Noutros casos efectua backtracking.<br>
     */

    public void call(String pn) throws Exception {
        int a = code.getInstrAddress(pn);
        if (a != -1) {
            registers.setCP(code.getAddrNextInstruction());
            numOfArgs = Util.arity(pn);
            registers.setB0(registers.getB());
            registers.setP(a);
        } else {
            write("Label '" + pn + "' not found", 0);
            fail = !backtrack();
        }
    }

    /**
     * Instrução execute p/n.<br><br>
     * <p/>
     * Se o P está definido, então guarda o <i>choise point’s address</i>
     * actual em B0 e continua a execução com a instrução de label P. <br>
     * Noutros casos efectua backtracking.<br>
     */

    public void execute(String p) throws Exception {
        int a = code.getInstrAddress(p);
        if (a != -1) {
            numOfArgs = Util.arity(p);
            registers.setB0(registers.getB());
            registers.setP(a);
        } else {
            backtrack();
        }
    }

    /**
     * Instrução proceed.<br><br>
     * <p/>
     * Continua a execução na instrução cujo endereço é
     * indicado pelo registo CP (continuation code).<br>
     */

    public void proceed() {
        registers.setP(registers.getCP());
    }

    /**
     * Instrução backtrack.<br><br>
     * <p/>
     * Efectua backtrack.<br>
     */

    public boolean backtrack() {
        int B = registers.getB();

        if (B <= stack.bottomOfStack() + 2) {
            fail = true;
            return false;
        } else {
            //registers.setB0(stack.getValue(B + stack.getValue(B) + 7));~
            registers.setP(stack.getValue(B + stack.getValue(B) + 4));
            return true;
        }
    }

    /**
     * Função auxiliar <i>deref address</i>.<br><br>
     * <p/>
     * De-referencia um endereço.<br>
     *
     * @param a endereço que se vai dereferenciar.
     */

    public int deref(int a) {
        int tag = store.getRegister(a).getTag();
        int value = store.getRegister(a).getValue();
        return (tag == REF && value != a) ? deref(value) : a;
    }

    /**
     * Função auxiliar <i>bind a1, a2: address</i>.<br><br>
     */

    public void bind(int a1, int a2) throws Exception {
        DataCell t1 = heap.getAddress(a1);
        DataCell t2 = heap.getAddress(a2);
        if ((t1.equals(REF)) && (!t2.equals(REF) || a2 < a1)) {
            store.setRegister(a1, store.getRegister(a2));
            trail(a1);
        } else if (t2.equals(REF)) {
            store.setRegister(a2, store.getRegister(a1));
            trail(a2);
        } else {
            throw new Exception("error: bind(" + a1 + "," + a2 + ")");
        }
    }

    /**
     * Função auxiliar <i>trail a: address</i>.<br><br>
     * <p/>
     * Efectua trail a um endereço.<br>
     *
     * @param a endereço ao qual se vai fazer trail.
     */

    public void trail(int a) throws MemoryOverflow {
        int HB = registers.getHB();
        int H = registers.getH();
        int B = registers.getB();
        int TR = registers.getTR();

        if (a < HB || (H < a && a < B)) {
            trail.add(a);
            registers.setTR(TR + 1);
        }
    }

    public void unWindTrail(int addr1, int addr2) {
        for (int i = addr1; i < addr2; i++) {
            store.setRegister(trail.get(i), new DataCell(trail.get(i)));
        }
    }

    public void tidyTrail() throws MemoryOverflow {
        int B = registers.getB();
        int HB = registers.getHB();
        int H = registers.getH();
        int TR = registers.getTR();
        int i = stack.getValue(B + stack.getValue(B) + 5);

        while (i < TR) {
            int tr = trail.get(i);
            if ((tr < HB) || ((H < tr) && (tr < B))) {
                i++;
            } else {
                trail.add(i, trail.get(TR - 1));
                registers.setTR(TR - 1);
            }
        }
    }

    public boolean unify(int a1, int a2) throws Exception {
        pdl.push(a1);
        pdl.push(a2);
        boolean fail = false;
        while (!(pdl.empty() || fail)) {
            int d1 = deref(pdl.pop());
            int d2 = deref(pdl.pop());
            if (d1 != d2) {
                DataCell c1 = store.getRegister(d1);
                DataCell c2 = store.getRegister(d2);
                if (c1.equals(REF)) {
                    bind(d1, d2);
                } else {

                    switch (c2.getTag()) {
                        case REF:
                            bind(d1, d2);
                            break;
                        case CON:
                            fail = ((!c1.equals(CON)) || (!c1.getStringValue().equals(c2.getStringValue())));
                            break;
                        case LIS: {
                            if (!c1.equals(LIS)) {
                                fail = true;
                            } else {
                                pdl.push(c1.getValue());
                                pdl.push(c2.getValue());
                                pdl.push(c1.getValue() + 1);
                                pdl.push(c2.getValue() + 1);
                            }
                        }
                        break;
                        case STR: {

                            if (!c1.equals(STR)) {
                                fail = true;
                            } else {

                                String f1 = store.getRegister(c1.getValue()).getStringValue();
                                String f2 = store.getRegister(c2.getValue()).getStringValue();
                                if (!f1.equals(f2)) {
                                    fail = true;
                                } else {
                                    int arity = Util.arity(f1);
                                    for (int i = 1; i <= arity; i++) {
                                        pdl.push(c1.getValue() + i);
                                        pdl.push(c2.getValue() + i);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return fail;
    }

    /**
     * O código compilado é guardado na <i>área de codigo</i>.<br>
     * É um array de palavras que consiste num possivel label,
     * um operador e os operandos que o seguem.
     */

    class Code extends Memory {

        /**
         * Endereço da última instrução de código.
         */
        private int lastInstruction;

        /**
         * Limite superior da área de código.
         */
        private int eofCodeArea;

        /**
         * Construtor da classe Code.
         *
         * @param upperlimit define o endereço que limita
         *                   superiormente a área de código.
         */

        public Code(int upperlimit) throws MemoryOverflow {
            lastInstruction = -1;
            if (upperlimit > 0) {
                eofCodeArea = upperlimit;
            } else {
                throw new MemoryOverflow("Code Area Overflow");
            }
        }

        /**
         * Procura o endereço associado a um label.
         *
         * @param label label do endereço que se procura.
         * @return Devolve o endereço associado ao label ou -1 se não for
         * encontrado.
         */

        public int getInstrAddress(String label) {
            write("Looking for label " + label + "\n", 2);
            for (int i = 0; i < lastInstruction; i++) {
                if (memory[i].compareToLabel(label)) {
                    write("Label @ 0x" + i + "\n", 2);
                    return i;
                }
            }
            write("Label not founded\n", 2);
            return -1;
        }

        /**
         * Obtém uma lista de todos os labels disponiveis na memória.
         *
         * @return Devolve uma lista de todos os labels disponiveis na memória.
         */

        public String getLabelsList() {
            Vector<String> labels = new Vector<String>();
            String s = "";
            for (int i = 0; i < lastInstruction; i++) {
                if (!memory[i].compareToLabel(" ") && !memory[i].compareToLabel("query$") && !labels.contains(memory[i].label())) {
                    labels.add(memory[i].label());
                }
            }

            for (int i = 0; i < labels.size(); i++) {
                s += "\n" + labels.get(i);
            }

            return (s.equals("")) ? "Any label found" : "Avaliable Labels:\n" + s;
        }

        public void resetQuery() {
            for (int i = 0; i < lastInstruction; i++) {
                if (memory[i].compareToLabel("query$")) {
                    lastInstruction = i;
                    for (int j = i; j < memory.length; j++) {
                        memory[j] = null;
                    }
                }
            }
        }

        /**
         * Adiciona código a área de código.
         *
         * @param source string com o código a ser adicionado.
         */

        public void addSourceCode(String source) {
            resetQuery();
            registers.setP(lastInstruction);
            StringTokenizer st = new StringTokenizer(source, ",\n\t \\");
            int tokens = st.countTokens();
            if (tokens > 0) {
                /** remove halt */
                if (lastInstruction > 0) {
                    lastInstruction--;
                }

                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (!token.endsWith("$") && !token.endsWith(":")) {
                        memory[++lastInstruction] = new DataCell(CON, token, lastInstruction);
                    } else {
                        memory[++lastInstruction] = new DataCell(CON, st.nextToken(), lastInstruction);
                        memory[lastInstruction].setLabel(token.replace(":", ""));
                    }
                }
                memory[++lastInstruction] = new DataCell(CON, "halt", lastInstruction);
                registers.setCP(lastInstruction);
            }
        }

        /**
         * Obtem o maior endereço de uma instrução definida na área de código.
         *
         * @return Devolve o maior endereço de uma instrução definida na área
         * de código.
         */

        public int getLastInstrAddr() {
            return lastInstruction;
        }

        /**
         * Obtém a instrução que tem o registo P apontar para si.
         *
         * @return Devolve a instrução que tem o registo P apontar para si.
         */

        public Instruction getInstruction() throws InvalidInstruction {
            int P = registers.getP();
            if (memory != null && memory[P] != null) {
                Instruction ins = new Instruction(memory[P].getStringValue());
                int j = instructionSize(P);
                for (int i = 1; i < j; i++) {
                    ins.addNewArg(memory[P + i].getStringValue());
                }

                return ins;
            }
            throw new InvalidInstruction("Any instruction found at 0x" + P);
        }

        /**
         * Obtém o tamanho de uma intrução guardada no endereço <i>address</i>.
         *
         * @param address endereço da instrução.
         * @return Devolve o tamanho da instrução no endereço <i>address</i>.
         */

        public int instructionSize(int address) throws InvalidInstruction {
            if (memory[address] == null) {
                throw new InvalidInstruction("There is no instruction at memory address 0x" + address);
            } else if (!memory[address].equals(CON)) {
                throw new InvalidInstruction("Invalid instruction at memory address 0x" + address + " " + memory[address].toString());
            } else if (keys.containsKey(memory[address].getStringValue())) {
                return INSTRUCTIONS_SIZE[keys.get(memory[address].getStringValue())];
            } else {
                throw new InvalidInstruction("Unknown instruction <" + memory[address].getStringValue() + ">");
            }
        }

        /**
         * Set instruction counter pointing to next inst.
         */
        public int getAddrNextInstruction() throws InvalidInstruction {
            int P = registers.getP();
            return P + instructionSize(P);
        }

        /**
         * Obtém o número de instruções existentes na área de código.
         *
         * @retrun Devolve o número de instruções existentes na área de código.
         */

        public int getStatementCount() throws InvalidInstruction {
            int p = 0;
            for (int i = 0; i < lastInstruction; ) {
                int j = instructionSize(i);
                for (int n = 0; n < j; n++) {
                    if (memory[i + n] == null || !memory[i + n].equals(CON)) {
                        throw new InvalidInstruction("Malformed instruction starting at memory address " + i);
                    }
                }
                i += j;
                p++;
            }
            return p;
        }

        /**
         * Cria uma string com uma representação do conteúdo da área de código.
         *
         * @return Devolve uma string com uma representação do conteúdo da área
         * de código.
         */

        public String getInformation() throws InvalidInstruction {
            int t = Util.nrDigits(memory.length);

            String s, number, number2, space = Util.createSpaces(t - 1);
            s = "\n[ POINTERS ][" + space + "ADDRESS" + space + "]\n\n";
            for (int i = 0; i <= lastInstruction; ) {
                number = "";

                for (int l = Util.nrDigits(i); l < t; l++) {
                    number += "0";
                }
                int j = instructionSize(i);
                if (registers.getP() == i && registers.getCP() == i) {
                    s += "   P & CP   ";
                } else if (registers.getP() == i) {
                    s += "     P      ";
                } else if (registers.getCP() == i) {
                    s += "     CP     ";
                } else {
                    s += Util.createSpaces(12);
                }

                number2 = "";
                for (int l = Util.nrDigits(i + j - 1); l < t; l++) {
                    number2 += "0";
                }

                s += "[" + number + i + "] - [" + number2 + (i + j - 1) + "] ";
                s += memory[i].label().trim() + "\t";

                for (int n = 0; n < j; n++) {
                    if (memory[i + n] == null || !memory[i + n].equals(CON)) {
                        throw new InvalidInstruction("Malformed instruction starting at memory address " + i);
                    }
                    s += memory[i + n].getStringValue() + " ";
                }

                i += j;
                s += "\n";
            }
            number = "";
            for (int l = Util.nrDigits(lastInstruction + 1); l < t; l++) {
                number += "0";
            }

            number2 = "";
            for (int l = Util.nrDigits(eofCodeArea); l < t; l++) {
                number2 += "0";
            }

            s += Util.createSpaces(12) + "[" + number + (lastInstruction + 1) + "] - [" + number2 + eofCodeArea + "]\tEmpty Code Space\n";

            return s;
        }
    }

    /**
     * HEAP
     */
    class Heap extends Memory {

        /**
         * Heap botton limit.
         */
        private int heapBottomLimit;

        /**
         * Heap upper limit.
         */
        private int heapUpperLimit;

        public Heap(int heapBottomLimit, int heapUpperLimit) {
            registers.setHB(heapBottomLimit);
            registers.setS(heapBottomLimit);
            registers.setH(heapBottomLimit);
            this.heapBottomLimit = heapBottomLimit;
            this.heapUpperLimit = heapUpperLimit;
        }

        public String getInformation() throws HeapException {
            int t = Util.nrDigits(memory.length), heapPointer = registers.getH(), S = registers.getS(), HB = registers.getHB();

            String s = "\n", number, number2;//, space = Util.createSpaces(t-1);
            for (int i = heapBottomLimit; i <= heapUpperLimit; ) {
                number = "";

                for (int l = Util.nrDigits(i); l < t; l++) {
                    number += "0";
                }

                if (heapPointer == i && S == i && HB == i) {
                    s += " H & S & HB ";
                } else if (heapPointer == i && HB == i) {
                    s += "   H & HB   ";
                } else if (heapPointer == i && S == i) {
                    s += "   H & S    ";
                } else if (S == i && HB == i) {
                    s += "   S & HB   ";
                } else if (heapPointer == i) {
                    s += "     H      ";
                } else if (S == i) {
                    s += "     S      ";
                } else if (HB == i) {
                    s += "     HB     ";
                } else {
                    s += Util.createSpaces(12);
                }

                int j = 0;
                while (heapPointer != i + j && S != i + j && HB != i + j && i + j <= heapUpperLimit && memory[i + j] == null) {
                    j++;
                }

                number2 = "";
                for (int l = Util.nrDigits(i + j - 1); l < t; l++) {
                    number2 += "0";
                }

                if (j == 0) {
                    s += "[" + number + i + "] - [" + number + i + "]\t";
                } else {
                    s += "[" + number + i + "] - [" + number2 + (i + j - 1) + "]\t";
                }

                if (j != 0 || (i + j <= heapUpperLimit && memory[i + j] == null)) {
                    s += "Empty Heap Space";
                } else {
                    int tag = memory[i].getTag();
                    if (tag == REF || tag == STR || tag == LIS || tag == CON || tag == FUN) {
                        s += memory[i].toString();
                    } else {
                        throw new HeapException("Malformed heap content at memory address " + i);
                    }
                }
                i += (j != 0) ? j : 1;
                s += "\n";
            }
            return s;
        }
    }

    /**
     * STACK
     */
    class LocalStack extends Memory {

        /**
         * Stack bottom limit.
         */
        private int stackBottomLimit;

        /**
         * Stack upper limit
         */
        private int stackUpperLimit;

        public LocalStack(int stackBottomLimit, int stackUpperLimit) {
            //int s = stackUpperLimit - stackBottomLimit;
            registers.setE(stackBottomLimit);
            registers.setCE(stackBottomLimit);
            registers.setB(stackBottomLimit + 2);
            registers.setB0(stackBottomLimit + 2);
            this.stackBottomLimit = stackBottomLimit;
            this.stackUpperLimit = stackUpperLimit;
        }

        public String getInformation() throws StackException {
            int t = Util.nrDigits(memory.length), E = registers.getE(), B = registers.getB(), cutPointer = registers.getB0(), CE = registers.getCE();

            String s = "\n", number, number2;//, space = Util.createSpaces(t-1);
            for (int i = stackBottomLimit; i <= stackUpperLimit; ) {
                number = "";

                for (int l = Util.nrDigits(i); l < t; l++) {
                    number += "0";
                }

                if (E == i && B == i && cutPointer == i) {
                    s += " E & B & B0 ";
                } else if (E == i && cutPointer == i) {
                    s += "   E & B0   ";
                } else if (E == i && B == i) {
                    s += "   E & B    ";
                } else if (E == i && CE == i) {
                    s += "   E & CE   ";
                } else if (B == i && cutPointer == i) {
                    s += "   B & B0   ";
                } else if (E == i) {
                    s += "     E      ";
                } else if (CE == i) {
                    s += "     CE     ";
                } else if (B == i) {
                    s += "     B      ";
                } else if (cutPointer == i) {
                    s += "     B0     ";
                } else {
                    s += Util.createSpaces(12);
                }

                int j = 0;
                while (E != i + j && B != i + j && cutPointer != i + j && i + j <= stackUpperLimit && memory[i + j] == null) {
                    j++;
                }

                number2 = "";
                for (int l = Util.nrDigits(i + j - 1); l < t; l++) {
                    number2 += "0";
                }

                if (j == 0) {
                    s += "[" + number + i + "] - [" + number + i + "]\t";
                } else {
                    s += "[" + number + i + "] - [" + number2 + (i + j - 1) + "]\t";
                }

                if (j != 0 || (i + j <= stackUpperLimit && memory[i + j] == null)) {
                    s += "Empty Stack Space";
                } else {
                    int tag = memory[i].getTag();
                    if (tag == REF || tag == STR || tag == LIS || tag == CON || tag == FUN) {
                        s += memory[i].toString();
                    } else {
                        throw new StackException("Malformed stack content at memory address 0x" + i);
                    }
                }

                i += (j != 0) ? j : 1;
                s += "\n";
            }
            return s;
        }

        public int bottomOfStack() {
            return stackBottomLimit;
        }

        public boolean moreChoicePoints() {
            int B = registers.getB();

            if (B <= stack.bottomOfStack() + 2) {
                return false;
            }
            return true;
        }
    }

    /**
     * TRAIL
     */
    class Trail extends Memory {

        /**
         * Trail bottom limit.
         */
        private int trailBottomLimit;

        public Trail(int trailBottomLimit) {
            registers.setTR(trailBottomLimit);
            this.trailBottomLimit = trailBottomLimit;
        }

        public void add(int address) {
            setAddress(registers.getTR(), address);
        }

        public void add(int address, int val) {
            setAddress(address, val);
        }

        public int get() {
            return getAddress(registers.getTR()).getValue();
        }

        public int get(int i) {
            return getAddress(i).getValue();
        }

        public String getInformation() throws Exception {
            int t = Util.nrDigits(memory.length);

            String s = "\n", number, number2;//, space = Util.createSpaces(t-1);
            for (int i = trailBottomLimit; i <= registers.getTR(); ) {//trailUpperLimit;){
                number = "";

                for (int l = Util.nrDigits(i); l < t; l++) {
                    number += "0";
                }

                if (registers.getTR() == i) {
                    s += "     TR     ";
                } else {
                    s += Util.createSpaces(12);
                }

                int j = 0;
                while (i + j <= registers.getTR() && memory[i + j] == null) {
                    j++;
                }

                number2 = "";
                for (int l = Util.nrDigits(i + j - 1); l < t; l++) {
                    number2 += "0";
                }

                if (j == 0) {
                    s += "[" + number + i + "] - [" + number + i + "]\t";
                } else {
                    s += "[" + number + i + "] - [" + number2 + (i + j - 1) + "]\t";
                }

                if (j != 0) {
                    s += "Empty Trail Space";
                } else if (memory[i].equals(REF)) {
                    s += "Reference: 0x" + memory[i].getValue();
                } else {
                    throw new Exception("Malformed trail content at memory address 0x" + i);
                }

                i += (j != 0) ? j : 1;
                s += "\n";
            }
            return s;
        }
    }

    /**
     * PDL
     */
    class PDL extends Memory {

        /**
         * PDL upper limit.
         */
        private int pdlUpperLimit;

        /**
         * PDL bottom limit.
         */
        private int pdlBottomLimit;

        public PDL(int pdlUpperLimit) throws MemoryOverflow {
            this.pdlUpperLimit = pdlBottomLimit = pdlUpperLimit;

            if (pdlBottomLimit <= registers.getTR()) {
                throw new MemoryOverflow("PDL Memory Overflow");
            }
        }

        public boolean empty() {
            return pdlBottomLimit == pdlUpperLimit;
        }

        public int pop() throws MemoryOverflow {
            if (pdlBottomLimit > pdlUpperLimit) {
                int v = memory[pdlUpperLimit++].getValue();
                memory[pdlUpperLimit - 1] = null;
                return v;
            } else {
                throw new MemoryOverflow("PDL Memory Overflow");
            }
        }

        public void push(int i) throws MemoryOverflow {
            if (pdlUpperLimit - 1 <= registers.getTR()) {
                throw new MemoryOverflow("PDL Memory Overflow");
            }
            memory[--pdlUpperLimit] = new DataCell(i);
        }

        public String getInformation() throws Exception {
            int t = Util.nrDigits(memory.length);
            String s = "", number, number2;//, space = Util.createSpaces(t-1);
            for (int i = registers.getTR() + 1; i <= pdlUpperLimit; ) {
                number = "";

                for (int l = Util.nrDigits(i); l < t; l++) {
                    number += "0";
                }

                if (pdlBottomLimit == i) {
                    s += "    PDL     ";
                } else {
                    s += Util.createSpaces(12);
                }

                int j = 0;
                while (i + j < pdlUpperLimit && memory[i + j] == null) {
                    j++;
                    if (i + j <= pdlUpperLimit && i + j == pdlBottomLimit) {
                        break;
                    }
                }

                number2 = "";
                for (int l = Util.nrDigits(i + j - 1); l < t; l++) {
                    number2 += "0";
                }

                if (j == 0) {
                    s += "[" + number + i + "] - [" + number + i + "]\t";
                } else {
                    s += "[" + number + i + "] - [" + number2 + (i + j - 1) + "]\t";
                }

                if (memory[i] == null && i == pdlBottomLimit) {
                    s += "Empty PDL Space";
                } else if (j != 0 || memory[i + j] == null) {
                    s += "Empty Trail/PDL Space";
                } else if (memory[i].equals(REF)) {
                    s += memory[i].toString();
                } else {
                    throw new Exception("Malformed PDL content at memory address 0x" + i);
                }

                i += (j != 0) ? j : 1;
                s += "\n";
            }
            return s;
        }
    }

    /**
     * STORE
     */
    class Store extends Memory {

        Store() {
        }

        public void setRegister(int key, DataCell a) {
            heap.setAddress(key, a);
        }

        public void setRegister(String key, DataCell a) throws Exception {
            registers.setRegister(key, a);
        }

        public DataCell getRegister(int key) {
            return heap.getAddress(key);
        }

        public DataCell getRegister(String key) throws Exception {
            return registers.getRegister(key);
        }
    }

    /**
     * Memória onde se encontram todos os dados.
     */
    class Memory {

        public DataCell getAddress(int address) {
            return memory[address];
        }

        public int getValue(int address) {
            int i = (memory[address].equals(REF)) ? memory[address].getValue() : -1;
            return i;
        }

        public void setAddress(int address, DataCell a) {
            memory[address] = a;
        }

        public void setAddress(int address, int a) {
            memory[address] = new DataCell(a);
        }
    }

    /**
     * Registos da WAM.
     */
    class Registers extends Memory {
        private int S;
        private int HB;
        private int H;
        private int B0;
        private int B;
        private int E;
        private int TR;

        /**
         * instruction counter - P
         * A global register P is always set to contain the address of the next instruction to
         * execute.
         * However, some instructions have for purpose to break the sequential order of
         * execution or to connect to some other instruction at the end of a sequence. These
         * instructions are called <i>control instructions</i> as they typically set P in a
         * non-standard way. This is the case of <i>call p/n</i>.
         */

        private int P;

        /**
         * cont. code
         * we will use another global register CP, along with P, set
         * to contain the address (in the code area) of the next instruction to follow up with
         * upon successful return from a call
         */

        private int CP;
        private int CE;

        private Vector<DataCell> RX;
        private Vector<DataCell> RA;

        public Registers() {
            S = 0;
            HB = 0;//null;
            H = 0;
            B0 = 0;
            B = 0; //-1
            E = 0;
            TR = 0;
            RX = new Vector<DataCell>();
            RA = new Vector<DataCell>();

            P = 0;//null;
            CP = 0;//null;
            CE = 0;//null;
        }

        public int getS() {
            return S;
        }

        public int getHB() {
            return HB;
        }

        public int getH() {
            return H;
        }

        public int getB0() {
            return B0;
        }

        public int getB() {
            return B;
        }

        public int getE() {
            return E;
        }

        public int getTR() {
            return TR;
        }

        public int getP() {
            return P;
        }

        public int getCP() {
            return CP;
        }

        public int getCE() {
            return CE;
        }

        public void setS(int n) {
            S = n;
        }

        public void setHB(int n) {
            HB = n;
        }

        public void setH(int n) {
            H = n;
        }

        public void setB0(int n) {
            B0 = n;
        }

        public void setB(int n) {
            B = n;
        }

        public void setE(int n) {
            E = n;
        }

        public void setTR(int n) {
            TR = n;
        }

        public void setP(int n) {
            P = n;
        }

        public void setCP(int n) {
            CP = n;
        }

        public void setCE(int n) {
            CE = n;
        }

        public DataCell getRegister(Vector<DataCell> reg, int i) {
            int length = reg.size();
            while (length++ < i + 1) {
                reg.addElement(new DataCell(CON, "null", length));
            }
            return (DataCell) reg.get(i);
        }

        public void setRegister(Vector<DataCell> reg, int i, DataCell d) {
            int length = reg.size();
            while (length++ < i + 1) {
                reg.addElement(new DataCell(CON, "null", length));
            }
            reg.set(i, new DataCell(CON, d.getStringValue(), d.getValue()));
            //System.out.println("adiciona " + d.getStringValue() + " - " + reg.get(0).toString());

        }

        public void setRegister(String register, DataCell d) {
            write("\nSET REGISTER " + register, 3);
            int index = Util.getRegisterIndex(register);
            switch (register.charAt(0)) {
                case 'A':
                    setRegister(RA, index, d);
                    break;
                case 'Y':
                    memory[E + index + 2] = d;
                    break;
                case 'X':
                    setRegister(RX, index, d);
                    break;
            }
        }

        public DataCell getRegister(String register) {
            int index = Util.getRegisterIndex(register);
            switch (register.charAt(0)) {
                case 'A':
                    return getRegister(RA, index);
                case 'Y':
                    return memory[E + index + 1];
                case 'X':
                    return getRegister(RX, index);
                default:
                    return new DataCell(CON, "null", 0);
            }
        }

        public String printRA() {
            String s = "";
            for (int i = 0; i < RA.size(); i++) {
                DataCell dc = getRegister(RA, i);
                String r = dc.getStringValue();
                if (dc.equals(CON) && (r == null || r.equals("null") || r.trim().equals(""))) {
                    r = "0x" + dc.getValue();
                }

                s += "\nA" + i + ": " + ((r == null || r.equals("null") || r.trim().equals("")) ? " " : r + " ");
            }
            return s;
        }

        public String toString() {
            String s = "\n\nREGISTERS:\n" + "\tCE [" + CE + "]\n" + "\tCP [" + CP + "]\n" + "\tP [" + P + "]\n" + "\tS [" + S + "]\n" + "\tHB [" + HB + "]\n" + "\tH [" + H + "]\n" + "\tB0 [" + B0 + "]\n" + "\tB [" + B + "]\n" + "\tE [" + E + "]\n" + "\tTR [" + TR + "]\n" + "\tA [ ";

            for (int i = 0; i < RA.size(); i++) {
                DataCell dc = getRegister(RA, i);
                String r = dc.getStringValue();
                if (dc.equals(CON) && (r == null || r.equals("null") || r.trim().equals(""))) {
                    r = "0x" + dc.getValue();
                }

                s += (r == null || r.equals("null") || r.trim().equals("")) ? " " : r + " ";
            }
            s += "]\n\tX [ ";

            for (int i = 0; i < RX.size(); i++) {
                String r = getRegister("X" + i).getStringValue();
                s += (r.equals("null")) ? "" : r + " ";
            }

            s += "]\n";

            return s;
        }
    }

    /**
     * Apresenta apenas a mensagem se o nivel de debug o permitir.<br><ul>
     * <li>0 - output minimo + erros</li>
     * <li>1 - instrucoes</li>
     * <li>2 - procedimentos na memória</li>
     * <li>3 - estados</li></ul>
     *
     * @param s          mensagem a apresentar.
     * @param debugLevel nivel de debug da mensagem.
     */

    public synchronized void write(String s, int debugLevel) {
        if (debugLevel <= debug) {
            System.out.print(s);
        }
    }

    /**
     * Termina a executação com apresentação de uma mensagem.
     */
    public void failAndExit(String s) {
        write(s, 0);
        System.exit(-1);
    }

    /**
     * Efectua o load do código para a memória.
     */
    public void loadSource(String s) {
        code.addSourceCode(s);
    }

    /**
     * Apresenta o conteúdo de toda a memória.
     */
    public void getMemInformation() {
        int d = debug;
        debug = 0;
        try {
            long ms = System.currentTimeMillis();
            if (code.getLastInstrAddr() == -1) {
                write("No program in memory.\n", 0);
            } else {
                write(code.getInformation(), 0);
                write(heap.getInformation(), 0);
                write(stack.getInformation(), 0);
                write(trail.getInformation(), 0);
                write(pdl.getInformation(), 0);
                write(registers.toString(), 0);
            }
            write("\n[" + code.getStatementCount() + " instructions in memory]\n", 0);

            write("Total time elapsed: " + (System.currentTimeMillis() - ms) + " ms.\n", 0);
            debug = d;
        } catch (MemoryOverflow e) {
            e.printStackTrace();
            System.out.println("Exception: " + e.getMessage());
            failAndExit("Try to increse program memory.\n\n[program terminated]");
        } catch (InvalidInstruction e) {
            e.printStackTrace();
            System.out.println("Exception: " + e.getMessage());
            failAndExit("Check instructions syntax.\n\n[program terminated]");
        } catch (HeapException e) {
            e.printStackTrace();
            System.out.println("Exception: " + e.getMessage());
            failAndExit("Check heap.\n\n[program terminated]");
        } catch (StackException e) {
            e.printStackTrace();
            System.out.println("Exception: " + e.getMessage());
            failAndExit("Check stack.\n\n[program terminated]");
        } catch (Exception e) {
            e.printStackTrace();

            System.out.println("Exception: " + e.getMessage());
            failAndExit("[program terminated]");
        }
    }

    /**
     * Apresenta todos os labels existentes na memória.
     */
    public void getAvaliableLabels() {
        int d = debug;
        debug = 0;
        write(code.getLabelsList() + "\n", 0);
        debug = d;
    }

    public void printARGS() {
        write(registers.printRA(), 0);
    }

    public void writeLn(String s) {
        System.out.println(s);
    }

    public void debug(String s, int b) {
    }
}
