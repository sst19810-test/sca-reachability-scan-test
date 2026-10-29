package demo.sast.cross;

import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;

public final class XmlImporter {
    // CWE-611: SAX parser keeps DTDs and external entities enabled
    public String text(InputStream xml) throws Exception {
        StringBuilder text = new StringBuilder();
        SAXParserFactory.newInstance().newSAXParser().parse(xml, new DefaultHandler() {
            @Override
            public void characters(char[] ch, int start, int length) {
                text.append(ch, start, length);
            }
        });
        return text.toString();
    }
}
