package nachos.ag;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.machine.StandardConsole;
import nachos.security.Privilege;
import nachos.threads.Semaphore;

/**
 * @author Kang Zhang
 *
 */
public class CoffGrader extends BasicTestGrader {

	private static final int ActionDone = 0;

	private static final int ActionFail = 1;

	private static final int ActionP = 2;

	private static final int ActionV = 3;

	private static final int ActionRead = 4;

	private static final int ActionStore = 5;

	private static final int ActionRandom = 6;

	private static final int ActionReadParameter = 7;

	private static final int NumSemaphore = 20;

	private static final int NumStoreValues = 20;

	private static final int Num_Parameter = 4;

	private static final String ParameterTag = "coffPar";

	private static final String InputText = "input";

	private static final String OutputText = "output";

	private static final String QuietMode = "quiet";

	private static final String MetricMode = "metric";

	private static final String TestDirectory = "testRoot";

	protected Semaphore[] semaphores;

	protected int[] storeValues = new int[NumStoreValues];

	protected ArrayList<Integer> params = new ArrayList<Integer>();

	protected EmbededConsole embededConsole;

	protected String testDirectory = null;
	protected File testRoot = null;

	protected boolean quietMode = false;

	protected boolean metricMode = false;

	@Override
	protected void init() {
		super.init();

		Lib.debug('g', "Coffgrader initialize");

		Arrays.fill(storeValues, 0);
		embededConsole = new EmbededConsole(super.privilege);
		super.privilege.machine.setConsole(embededConsole);

		for( int i = 0 ; i < Num_Parameter ; i++){
			if( hasArgument(ParameterTag+i) )
				params.add(getIntegerArgument(ParameterTag + i));
			else
				break;
		}

		if( hasArgument(TestDirectory) )
			testDirectory = getStringArgument(TestDirectory);

		super.privilege.doPrivileged(new Runnable(){
			public void run() {
				if(testDirectory == null)
					testRoot = new File(new File("").getAbsoluteFile().getParentFile(),"test");
				else
					testRoot = new File(testDirectory);
			}
		});

		if(hasArgument(InputText))
			embededConsole.in.append(loadFromFile(getStringArgument(InputText)));

		if(hasArgument(OutputText))
			embededConsole.out.append(loadFromFile(getStringArgument(OutputText)));

		if(hasArgument(QuietMode))
			quietMode = getBooleanArgument(QuietMode);

		if(hasArgument(MetricMode))
			metricMode = getBooleanArgument(MetricMode);


	}

	private FileReader fileReader = null;
	/* load a file's content from your disk */
	private String loadFromFile(final String fileName) {
		super.privilege.doPrivileged(new Runnable(){
			public void run() {
				try {
					fileReader = new FileReader(new File(testRoot,fileName));
				} catch (FileNotFoundException e) {
					fileReader = null;
				}
			}
		});

		Lib.assertTrue(fileReader != null,"Load file:"+fileName+" failed");

		StringBuffer sb = new StringBuffer();
		int b = -1;
		try {
			while((b = fileReader.read()) != -1){
				sb.append((char)b);
			}
			fileReader.close();
		} catch (IOException e) {
			Lib.assertNotReached("File read/close error");
		}

		return sb.toString();
	}

	/* Hook on exception handler */
	@Override
	public boolean exceptionHandler(Privilege privilege) {
		super.exceptionHandler(privilege);
		Processor processor = Machine.processor();
		int cause = processor.readRegister(Processor.regCause);
		
		if (cause != Processor.exceptionSyscall
				|| processor.readRegister(Processor.regV0) != -1)
			return true;

		int result = handleTestSystemCall(processor
				.readRegister(Processor.regA0), processor
				.readRegister(Processor.regA1), processor
				.readRegister(Processor.regA2), processor
				.readRegister(Processor.regA3));
		processor.writeRegister(Processor.regV0, result);
		processor.advancePC();

		return false;
	}

	/* Handle the test framework system call*/
	protected int handleTestSystemCall(int type, int a0, int a1, int a2) {
		switch (type) {
		case ActionDone:
			Lib.assertTrue(embededConsole.outputMatched,"Test failed, mismatched the output");
			done();
			Lib.assertNotReached(" Test has been ended");
			break;
		case ActionFail:
			System.out.println("Test failed");
			Machine.halt();
			break;
		case ActionP:
			checkSemIndex(a0);
			semaphores[a0].P();
			break;
		case ActionV:
			checkSemIndex(a0);
			semaphores[a0].V();
			break;
		case ActionRead:
			checkStoreIndex(a0);
			return storeValues[a0];
		case ActionStore:
			checkStoreIndex(a0);
			storeValues[a0] = a1;
			break;
		case ActionRandom:
			Lib.assertTrue(a0 > 0, "Invalid random range");
			return Lib.random(a0);
		case ActionReadParameter:
			Lib.assertTrue(a0 >= 0 && a0 < params.size(),
					"Invalid parameter index"+a0+" .Maybe exceed "+params.size()+" ?");
			return params.get(a0);
		default:
			Lib.assertNotReached("Unknow system call. ("+type+")");
			break;
		}
		return 0;
	}

	protected void checkSemIndex(int a0) {
		Lib.assertTrue(a0 >= 0 && a0 < NumSemaphore,
				"Invalid semaphone index:(" + a0 + ")");
	}

	protected void checkStoreIndex(int a0) {
		Lib.assertTrue(a0 >= 0 && a0 < NumStoreValues, "Invalid store index:("
				+ a0 + ")");
	}

	@Override
	protected void run() {
		semaphores = new Semaphore[NumSemaphore];
		for (int i = 0; i < NumSemaphore; i++)
			semaphores[i] = new Semaphore(0);
		super.run();
	}

	/* EmbededConsole, a modified console used to support standard console	 */
	protected class EmbededConsole extends StandardConsole {
		public StringBuffer in = new StringBuffer();

		public StringBuffer out = new StringBuffer();

		public boolean outputMatched = true;

		private int inOffset = 0;

		private int outOffset = 0;

		public EmbededConsole(Privilege privilege) {
			super(privilege);
		}

		protected int in() {
			if (inOffset >= in.length())
				return -1;
			else
				return in.charAt(inOffset++);

		}
		
		protected void out(int value) {
			if( !quietMode )
				super.out(value);
            if(outOffset >= out.length() || out.charAt(outOffset++) != value)
                outputMatched = false;

		}
	}
}
