
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Short program that prompts the user for a file and then determines if the
 * text in the file matches the grammar implemented in the private class
 * BNFParser
 
 * The definition of that grammer is reproduced here: 
 * BNFParser checks if a given program string matches the following grammar:
 * &lt sentence &gt =&gt &lt expression &gt !
 * <p>
 * &lt expression &gt =&gt &lt term &gt { (+|-)&lt term &gt}
 * <p>
 * &lt term &gt =&gt &lt factor &gt {(*|/) &lt factor &gt}
 * <p>
 * &lt factor &gt =&gt ( &lt expression &gt ) | identifier
 * <P>
 * where 'identifier' is the set of characters A-Z & a-z.
 * 
 * @author mboni
 *
 */
public class BNFDemoParser {

/**
* Main loop of the program, continually prompting the user until they quit
*/
	public static void main(String[] args) {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
			while ((run(br)) == true)
				;
		} catch (IOException e) {
			System.err.println("Something is wrong with the stdIn and its BufferedReader");
			e.printStackTrace();
		}
	}

	/**
	 * The real high level meat of the program:
	 * <p>
	 * prompt the user for a file
	 * <p>
	 * read the file and extract the text from it in a single string TODO/Note:
	 * can't be too huge a file or the string won't fit.
	 * <p>
	 * parse the program string from the file to confirm it matches the grammar.
	 * <p>
	 * exit if the user chooses to exit
	 * <p>
	 * 
	 * @param stdin
	 *            System.in wrapped in a BufferedReader
	 * @return true if the user wants to keep running files through the program
	 * @throws IOException
	 */
	public static boolean run(BufferedReader stdin) throws IOException {
		File input;
		// loop until an input file is given (or system exits)
		while ((input = getInputFile(stdin)) == null)
			;

		String s = getFileString(input);
		if (s == null) {
			return true;
		}
		new BNFParser(s).matches();
		System.out.println("Press the return/enter key to try a differnet file (type \"exit\" to quit).");
		s = stdin.readLine().trim();
		while (!s.equals("") && !s.equals("exit")) {
			System.out.println("Press the return/enter key to try a differnet file (type \"exit\" to quit).");
			s = stdin.readLine().trim();
		}
		if (s.equals("exit"))
			System.exit(0);
		return true;
	}

	/**
	 * Prompt the user for a file, until a valid file is given
	 * 
	 * @param stdin
	 *            BufferedReader wrapping System.in
	 * @return the file the user wants to use, null if an invalid file is given
	 * @throws IOException
	 */
	private static File getInputFile(BufferedReader stdin) throws IOException {
		System.out.println("Please input the file name:");
		String input = stdin.readLine();
		if (input.equals("exit"))
			System.exit(0);

		File readFile = new File(input);
		if (readFile.exists())
			if (input.endsWith(".txt"))
				return readFile;
			else
				System.out.println(input + " is not a .txt file.");
		else
			System.out.println(input + " is not a file.");
		return null;
	}

	/**
	 * Read a file witha buffered reader and return the contents of the file as
	 * a string.
	 * 
	 * @param input
	 *            the file to read in and make a string from
	 * @return
	 */
	private static String getFileString(File input) {
		try (BufferedReader br = new BufferedReader(new FileReader(input))) {
			String file = "";
			String line = "";
			while ((line = br.readLine()) != null)
				file += line;
			return file;
		} catch (FileNotFoundException e) {
			System.out.println("The file: " + input.getAbsolutePath() + " was not found. Try again.");
			return null;
		} catch (IOException e) {
			System.out.println("There was an IO problem. Try again.");
			return null;
		}
	}

	/**
	 * BNFParser checks if a given program string matches the following grammar:
	 * &lt sentence &gt =&gt &lt expression &gt !
	 * <p>
	 * &lt expression &gt =&gt &lt term &gt { (+|-)&lt term &gt}
	 * <p>
	 * &lt term &gt =&gt &lt factor &gt {(*|/) &lt factor &gt}
	 * <p>
	 * &lt factor &gt =&gt ( &lt expression &gt ) | identifier
	 * <P>
	 * where 'identifier' is the set of characters A-Z & a-z.
	 */
	private static class BNFParser {
		static enum Token {
			ADD_OP, MULT_OP, OPEN_PAR, CLOSE_PAR, END, ID, OTHER, TERMINATION
		}

		private int place_in_program = 0;
		private Token nextToken;
		private final String program;

		BNFParser(String p) {
			program = p;
		}

		boolean matches() {
			lex();
			if (sentence()) {
				System.out.println("The file contains a valid program");
				return true;
			} else {
				System.out.println("The file does not contain a valid program");
				return false;
			}
		}

		Token lex() {
			if (place_in_program >= program.length()) {
				System.out.println("Call Scanner...<?>");
				return Token.TERMINATION;
			}
			char c = program.charAt(place_in_program++);
			System.out.println("Call Scanner...<" + c + ">");
			if (c == '+' || c == '-')
				nextToken = Token.ADD_OP;
			else if (c == '*' || c == '/')
				nextToken = Token.MULT_OP;
			else if (c == '(')
				nextToken = Token.OPEN_PAR;
			else if (c == ')')
				nextToken = Token.CLOSE_PAR;
			else if (c == '!')
				nextToken = Token.END;
			/* 65-90 is A-Z for chars, and 97-122 is a-z */
			else if ((c >= 65 && c <= 90) || (c >= 97 && c <= 122))
				nextToken = Token.ID;
			else
				nextToken = Token.OTHER;
			return nextToken;
		}

		/**
		 * @return &ltsentence&gt =&gt &ltexpression&gt !
		 */
		boolean sentence() {
			enter("sentence");
			boolean ret = expression();
			if (ret) {
				if (nextToken == Token.END) {
					exit("sentence");
					return true;
				} else {
					badToken(nextToken, "!", program.charAt(place_in_program - 1));
					return false;
				}
			}
			return false;
		}

		/**
		 * @return &ltexpression&gt =&gt &ltterm&gt { (+|-)&ltterm&gt}
		 */
		boolean expression() {
			enter("expression");
			boolean ret = term();
			if (ret) {
				if (nextToken == Token.ADD_OP) {
					lex();
					ret = term();
					if (!ret) {
						return false;
					}
				}
				exit("expression");
				return true;
			}
			return false;
		}

		/**
		 * @return &ltterm&gt =&gt &ltfactor&gt {(*|/) &ltfactor&gt}
		 */
		boolean term() {
			enter("term");
			boolean ret = factor();
			if (ret) {
				if (nextToken == Token.MULT_OP) {
					lex();
					ret = factor();
					if (!ret)
						return false;
				}
				exit("term");
				return true;
			}
			return false;

		}

		/***
		 * @return &ltfactor&gt =&gt ( &ltexpression&gt ) | identifier
		 */
		boolean factor() {
			enter("factor");
			if (nextToken == Token.ID) {
				lex();
				exit("factor");
				return true;
			} else if (nextToken == Token.OPEN_PAR) {
				lex();
				boolean ret = expression();
				if (ret) {

					if (nextToken == Token.CLOSE_PAR) {
						lex();
						exit("factor");
						return true;
					} else
						badToken(nextToken, ")", program.charAt(place_in_program - 1));
				}

			} else
				badToken(nextToken, "(", program.charAt(place_in_program - 1));
			return false;
		}

		/**
		 * print an error statement, showing the token that was parsed, the
		 * character it came from, and what characters were valid
		 */
		private void badToken(Token t, String validCharsForToken, char c) {
			System.out.println(
					"Token: " + t + " not one of the valid characters: \"" + validCharsForToken + "\" for char: " + c);
		}

		/**
		 * print "Entering &lts&gt..."
		 * 
		 * @param s
		 */
		private void enter(String s) {
			System.out.println("Entering <" + s + ">...");
		}

		/**
		 * print "Exiting &lts&gt..."
		 * 
		 * @param s
		 */
		private void exit(String s) {
			System.out.println("Exiting <" + s + ">...");
		}
	}
}
