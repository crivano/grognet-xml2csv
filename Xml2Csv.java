import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class Xml2Csv {

	private static final char DEFAULT_SEPARATOR = ';';

	public static void writeLine(Writer w, List<String> values) throws IOException {
		writeLine(w, values, DEFAULT_SEPARATOR, ' ');
	}

	public static void writeLine(Writer w, List<String> values, char separators) throws IOException {
		writeLine(w, values, separators, ' ');
	}

	// https://tools.ietf.org/html/rfc4180
	private static String followCVSformat(String value) {

		String result = value;
		if (result == null)
			return "";
		if (result.contains("\"")) {
			result = result.replace("\"", "\"\"");
		}
		return result;

	}

	public static void writeLine(Writer w, List<String> values, char separators, char customQuote) throws IOException {

		boolean first = true;

		// default customQuote is empty

		if (separators == ' ') {
			separators = DEFAULT_SEPARATOR;
		}

		StringBuilder sb = new StringBuilder();
		for (String value : values) {
			if (!first) {
				sb.append(separators);
			}
			if (customQuote == ' ') {
				sb.append(followCVSformat(value));
			} else {
				sb.append(customQuote).append(followCVSformat(value)).append(customQuote);
			}

			first = false;
		}
		sb.append("\n");
		w.append(sb.toString());

	}

	static Set<File> listFiles(Set<File> set, String directoryName) {
		if (set == null)
			set = new TreeSet<>();
		File directory = new File(directoryName);
		File[] fList = directory.listFiles();
		for (File file : fList) {
			if (file.isFile()) {
				if (file.getName().toLowerCase().endsWith(".xml"))
					set.add(file);
			} else if (file.isDirectory()) {
				listFiles(set, file.getAbsolutePath());
			}
		}
		return set;
	}

	static String load(String current, XMLStreamReader xmlr, String field) throws XMLStreamException {
		if (xmlr.isStartElement() && field.equals(xmlr.getLocalName())) {
			StringBuilder sb = new StringBuilder();

			while (xmlr.hasNext()) {
				xmlr.next();
				if (xmlr.isCharacters())
					sb.append(xmlr.getText());
				if (xmlr.isEndElement())
					break;
			}
			return sb.toString();
		}
		return current;
	}

	private static class Row {
		String numProcCompl;
		String numProcComplAnt;
		String descr;
		String codLoc;
		String txt;
		String indCit;
		String dtIndCit;
		String dtFimIndCit;
		String codAssProc;
		String codClass;
		String valCausa;
		String dtValCausa;
		String dtAutua;

		String codTipoAutuac;
		String nome;
		String numDocPess;

		String cda;

		static Pattern reEmenta = Pattern.compile("E ?M ?E ?N ?T ?A");
		static Pattern reDarProvimento = Pattern.compile("DAR PROVIMENTO");
		static Pattern reNegarProvimento = Pattern.compile("NEGAR PROVIMENTO");
		static Pattern reParcialProvimento = Pattern.compile("PARCIAL PROVIMENTO");
		static Pattern reNegarSeguimento = Pattern.compile("NEGAR SEGUIMENTO");
		static Pattern reNaoConhecido = Pattern.compile("N[AÃ]O CONHECER");

		boolean isEmenta() {
			if (txt == null)
				return false;
			return reEmenta.matcher(txtInicial()).find();
		}

		boolean isDarProvimento() {
			if (txt == null)
				return false;
			return reDarProvimento.matcher(txtFinal()).find();
		}

		boolean isNegarProvimento() {
			if (txt == null)
				return false;
			return reNegarProvimento.matcher(txtFinal()).find();
		}

		boolean isParcialProvimento() {
			if (txt == null)
				return false;
			return reParcialProvimento.matcher(txtFinal()).find();
		}

		boolean isNegarSeguimento() {
			if (txt == null)
				return false;
			return reNegarSeguimento.matcher(txtFinal()).find();
		}

		boolean isNaoConhecido() {
			if (txt == null)
				return false;
			return reNaoConhecido.matcher(txtFinal()).find();
		}

		private String txtInicial() {
			return txt.substring(0, txt.length() >= 100 ? 100 : txt.length()).toUpperCase();
		}

		private String txtFinal() {
			return txt.substring(txt.length() > 700 ? txt.length() - 700 : 0).toUpperCase();
		}
	}

	static void writeCsvLine(Row row, Writer w) throws IOException {
		writeLine(w,
				Arrays.asList(row.numProcCompl, row.numProcComplAnt, row.descr, row.codLoc, row.indCit, row.dtIndCit,
						row.dtFimIndCit, row.codAssProc, row.codClass, row.valCausa, row.dtAutua, row.codTipoAutuac,
						row.nome, row.numDocPess, row.cda, row.isEmenta() ? "S" : "N",
						row.isDarProvimento() ? "S" : "N", row.isNegarProvimento() ? "S" : "N",
						row.isParcialProvimento() ? "S" : "N", row.isNegarSeguimento() ? "S" : "N",
						row.isNaoConhecido() ? "S" : "N"));
	}

	static void processFile(File file, Writer writer) throws IOException, XMLStreamException {
		System.out.println(file.getAbsolutePath());

		XMLInputFactory xmlif = XMLInputFactory.newInstance();
		XMLStreamReader xmlr = xmlif.createXMLStreamReader(file.getName(), new FileInputStream(file));

		Row row = null;
		while (xmlr.hasNext()) {
			xmlr.next();
			if (xmlr.isStartElement()) {
				if ("ROW".equals(xmlr.getLocalName())) {
					if (row != null) {
						writeCsvLine(row, writer);
						row = null;
					}
					row = new Row();
					continue;
				}
				if (row == null)
					continue;
				row.numProcCompl = load(row.numProcCompl, xmlr, "NUMPROCCOMPL");
				row.numProcComplAnt = load(row.numProcComplAnt, xmlr, "NUMPROCCOMPLANT");
				row.descr = load(row.descr, xmlr, "DESCR");
				row.codLoc = load(row.codLoc, xmlr, "CODLOC");
				row.txt = load(row.txt, xmlr, "TXT");
				row.indCit = load(row.indCit, xmlr, "INDCIT");
				row.dtIndCit = load(row.dtIndCit, xmlr, "DTINDCIT");
				row.dtFimIndCit = load(row.dtFimIndCit, xmlr, "DTFIMINDCIT");
				row.codAssProc = load(row.codAssProc, xmlr, "CODASSPROC");
				row.codClass = load(row.codClass, xmlr, "CODCLASS");
				row.valCausa = load(row.valCausa, xmlr, "VALCAUSA");
				row.dtValCausa = load(row.dtValCausa, xmlr, "DTVALCAUSA");
				row.dtAutua = load(row.dtAutua, xmlr, "DTAUTUA");

				row.codTipoAutuac = load(row.codTipoAutuac, xmlr, "CODTIPATUAC");
				row.nome = load(row.nome, xmlr, "NOME");
				row.numDocPess = load(row.numDocPess, xmlr, "NUMDOCPESS");

				row.cda = load(row.cda, xmlr, "CDA");
			}
		}
		if (row != null) {
			writeCsvLine(row, writer);
		}
	}

	static void processFiles(String directory, String csvFile) throws IOException, XMLStreamException {
		Set<File> files = listFiles(null, directory);
		FileWriter writer = new FileWriter(csvFile);

		writeLine(writer,
				Arrays.asList("numProcCompl", "numProcComplAnt", "descr", "codLoc", "indCit", "dtIndCit", "dtFimIndCit",
						"codAssProc", "codClass", "valCausa", "dtAutua", "codTipoAutuac", "nome", "numDocPess", "cda",
						"ementa", "darProvimento", "negarProvimento", "parcialProvimento", "negarSeguimento",
						"naoConhecido"));

		for (File file : files)
			processFile(file, writer);

		writer.flush();
		writer.close();
	}

	public static void main(String[] args) throws IOException, XMLStreamException {
		if (args == null || args.length != 2)
			System.out.println(
					"Grognet Xml2Csv\n\nParâmetro 1: diretório onde estão os arquivos XML\nParâmetro 2: nome do arquivo CSV que será gerado\n\nExemplo: C:\\grognet-xml2csv c:\\XMLs\\ c:\\temp\\teste.csv");
		String directory = args[0];
		String csvFile = args[1];

		processFiles(directory, csvFile);
	}
}
