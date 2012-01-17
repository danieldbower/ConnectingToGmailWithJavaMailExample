package emailtest;

import static org.junit.Assert.*

import javax.mail.Folder
import javax.mail.Store

import org.junit.*

/**
 * No real testing going on here, just exercising the Application Class
 * @author daniel
 *
 */
class ApplicationTest {

	private Application app
	
	@Before
	public void setup(){
		app = new Application("/tmp/emailtest.properties")
	}
	
	@After
	public void teardown(){
		app.close()
	}
	
	
	@Test
	public void testSmtp() {
		app.send("test Subject", "test Message");
	}
	
	@Test
	public void testImap() {
		//list the folders in the store
		app.listFolders()
		
		//get the Inbox folder
		Folder readOnlyInboxfolder = app.folderBy("INBOX", true)
		
		//count messages in the inbox
		app.countMessages(readOnlyInboxfolder)
		
		//display subject lines in the inbox folder
		app.listMessages(readOnlyInboxfolder)
	}

}
