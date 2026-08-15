import org.phylospec.runner.PhyloSpecRunner;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Test2 {

    static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
        String source = """
              String file = "/Users/ochsneto/Documents/PhyloSpec/rev-anaylsis/primate.nex"
              Alignment data = fromNexus(file)
        """;

        PhyloSpecRunner<?, ?> parser = new Beast3Runner(source);
        parser.runPhyloSpec("Test2");
    }

}
