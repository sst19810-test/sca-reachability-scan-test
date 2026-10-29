package demo.sast.cross;

import java.io.InputStream;

/** Pass-through layer so every tainted value crosses at least two file boundaries. */
public final class AdminService {
    private final AccountRepository accounts = new AccountRepository();
    private final ShellRunner shell = new ShellRunner();
    private final DocumentStore documents = new DocumentStore();
    private final XmlImporter xml = new XmlImporter();
    private final HttpFetcher http = new HttpFetcher();
    private final SessionCodec sessions = new SessionCodec();

    public String accountsPage(String owner) throws Exception {
        return "<h1>Accounts for " + owner + "</h1>" + accounts.findByOwner(owner);
    }

    public String backup(String target) throws Exception {
        return shell.run("tar czf /var/backups/" + target + ".tgz /var/app/data");
    }

    public void saveNote(String name, String body) throws Exception {
        documents.write(name, body);
    }

    public String importCatalog(InputStream body) throws Exception {
        return xml.text(body);
    }

    public String preview(String url) throws Exception {
        return http.get(url);
    }

    public Object preferences(String encoded) throws Exception {
        return sessions.decode(encoded);
    }
}
