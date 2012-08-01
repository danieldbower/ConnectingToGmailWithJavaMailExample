package emailtest

import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Application {

	public static void main(String[] args){
		if(args.size()<2){
			logger.error "You'll need to provide arguments: \nConfig File Path, action (s send via smtp, f fetch via imap)"
			return
		}
		
		Application app = new Application(args[0])
		
		if(args[1]=="s"){
			if(args.size()<4){
				logger.error "You'll need to provide arguments for the subject and the message, optionally, you can provide the sender and receiver"
				return
			}else{
				if(args.size()==6){
					logger.debug "Sending from ${args[4]} to ${args[5]} with subject: \n${args[2]} \nAnd body: \n${args[3]}"
					app.send(args[2], args[3], args[4], args[5])
				}else{
					logger.debug "Sending from smtp user to imap user with subject: \n${args[2]} and body: \n${args[3]}"
					app.send(args[2], args[3])
				}
			}
		}else if(args[1]=="f"){
			if(args.size()<3){
				logger.info "You can provide an argument to fetch the contents of an imap folder"
				app.listFolders()
			}else{
				Folder folder = app.folderBy(args[2], true)
				app.countMessages(folder)
				app.listMessages(folder)
			}
		}
		
		app.close();
	}

	public Application(String configFileLocation){
		loadConfig(configFileLocation);
	}

	/**
	 * Send a message from the smtp user to the imap user
	 */
	public void send(String subj, String mesg){
		send(subj, mesg, smtpUsername, imapUsername)
	}
	
	public void send(String subj, String mesg, String sender, String receiver){
		gmailSmtpConnect()
		
		Message message = new MimeMessage(smtpSession);
		message.setFrom(new InternetAddress(sender))
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiver))
		message.setSubject(subj)
		message.setText(mesg)

		transport.sendMessage(message, message.getAllRecipients())

		logger.info "Message sent."
	}

	/**
	 * Get a folder from the store in order to work with it in the rest of the api
	 */
	protected Folder folderBy(String folderName, boolean readOnly){
		gmailImapConnect()
		
		Folder folder = store.getFolder(folderName)
		if(readOnly){
			folder.open(Folder.READ_ONLY)
		}else{
			folder.open(Folder.READ_WRITE)
		}
		return folder
	}

	/**
	 * Count the Messages in the given folder
	 * @param folder
	 */
	public void countMessages(Folder folder){
		logger.info "\n\nNumber of Messages in folder:  " + folder.messageCount
	}

	/**
	 * List the Folders
	 */
	public void listFolders(){
		gmailImapConnect()
		
		StringBuilder sb = new StringBuilder()
		sb.append "\n\nFolders:"
		store.defaultFolder.list().each { folder ->
			sb.append "\n" + folder.name
		}
		logger.info sb.toString()
	}

	/**
	 * List the Messages in the given folder
	 */
	public void listMessages(Folder folder){
		StringBuilder sb = new StringBuilder()
		sb.append "\n\nMessages in Folder:"
		
		folder.messages.each { msg ->
			sb.append "\n" + msg.subject
		}
		logger.info sb.toString()
	}

	private Properties props

	private String imapHost
	private String imapPort
	private String imapUsername
	private String imapPassword
	
	private String smtpHost
	private int smtpPort
	private boolean smtpSsl = true
	private String smtpUsername
	private String smtpPassword

	private Session imapSession
	private Store store

	private Session smtpSession
	private Transport transport

	private static final Logger logger = LoggerFactory.getLogger("emailtest.Application")

	/**
	 * Connect to Gmail via SMTP
	 */
	protected Transport gmailSmtpConnect(){
		if(!transport){
			Properties props = new Properties()
			if(smtpSsl){
				props.setProperty("mail.smtps.auth", "true")
			}

			smtpSession = Session.getInstance(props, null)
			transport = smtpSession.getTransport(smtpSsl?"smtps":"smtp")
			transport.connect(smtpHost, smtpPort, smtpUsername, smtpPassword)
		}

		return transport
	}

	/**
	 * Connect to Gmail via IMAP
	 */
	protected Store gmailImapConnect(){
		if(!store){
			Properties props = new Properties()
			props.setProperty("mail.store.protocol", "imaps")
			props.setProperty("mail.imaps.host", imapHost)
			props.setProperty("mail.imaps.port", imapPort)

			imapSession = Session.getInstance(props,null)
			store = imapSession.getStore("imaps")
			store.connect(imapUsername, imapPassword)
		}

		return store
	}

	/**
	 * Close out all outstanding connections when you are done
	 */
	public void close(){
		if(store) store.close()
		if(transport) transport.close()
	}

	/**
	 * Simple method to load the configuration from the file system instead of 
	 * broadcasting my email password to the world.
	 */
	private void loadConfig(String configFileLocation){
		File file = new File(configFileLocation)
		if(!(file.exists() && file.canRead())){
			logger.error ("Unable to load config.properties from {}", configFileLocation)
		}else{
			props = new Properties()
			props.load(file.newInputStream())
			logger.info("Read Configuration file from {}", configFileLocation)

			imapHost 	 =  props["imap_host"]
			imapPort 	 =  props["imap_port"]
			imapUsername =  props["imap_username"]
			imapPassword =  props["imap_password"]
			
			smtpHost	 =  props["smtp_host"]
			smtpPort	 =  Integer.valueOf(props["smtp_port"])
			if(props["smtp_ssl_on"]!=null){
				smtpSsl		 =  Boolean.valueOf(props["smtp_ssl_on"])
			}
			smtpUsername =  props["smtp_username"]
			smtpPassword =  props["smtp_password"]
		}
	}
}
