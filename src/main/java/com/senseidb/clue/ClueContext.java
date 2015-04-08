package com.senseidb.clue;

import java.io.IOException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.senseidb.clue.commands.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

import com.senseidb.clue.api.BytesRefDisplay;
import com.senseidb.clue.api.IndexReaderFactory;
import com.senseidb.clue.api.QueryBuilder;

public class ClueContext {

  private final IndexReaderFactory readerFactory;
  private final SortedMap<String, ClueCommand> cmdMap;
  private final boolean interactiveMode;
  private IndexWriter writer;
  private final Directory directory;
  private boolean readOnlyMode;
  private final IndexWriterConfig writerConfig;
  private final QueryBuilder queryBuilder;
  private final Analyzer analyzerQuery;
  private final BytesRefDisplay termBytesRefDisplay;
  private final BytesRefDisplay payloadBytesRefDisplay;
  
  public ClueContext(Directory dir, ClueConfiguration config, boolean interactiveMode) 
      throws Exception {
    this.directory = dir;
    this.analyzerQuery = config.getAnalyzerQuery();
    this.readerFactory = config.getIndexReaderFactory();
    this.readerFactory.initialize(directory);
    this.queryBuilder = config.getQueryBuilder();
    this.queryBuilder.initialize("contents", analyzerQuery);
    this.writerConfig = new IndexWriterConfig(new StandardAnalyzer());
    this.termBytesRefDisplay = config.getTermBytesRefDisplay();
    this.payloadBytesRefDisplay = config.getPayloadBytesRefDisplay();
    this.writer = null;
    this.interactiveMode = interactiveMode;
    this.cmdMap = new TreeMap<String, ClueCommand>();
    // default to readonly
    this.readOnlyMode = true;
    
    // registers all the commands we currently support
    new HelpCommand(this);
    new ExitCommand(this);
    new InfoCommand(this);
    new DocValCommand(this);
    new SearchCommand(this);
    new TermsCommand(this);
    new PostingsCommand(this);
    new DocSetInfoCommand(this);
    new MergeCommand(this);
    new DeleteCommand(this);
    new ReadonlyCommand(this);
    new DirectoryCommand(this);
    new ExplainCommand(this);
    new NormsCommand(this);
    new TermVectorCommand(this);
    new StoredFieldCommand(this);
    new ReconstructCommand(this);
    new ExportCommand(this);
    new IndexTrimCommand(this);
    new GetUserCommitDataCommand(this);
    new SaveUserCommitData(this);
    new DeleteUserCommitData(this);
    new DumpDocCommand(this);
  }
  
  
  public QueryBuilder getQueryBuilder() {
    return queryBuilder;
  }

  public Analyzer getAnalyzerQuery() {
    return analyzerQuery;
  }
  
  public BytesRefDisplay getTermBytesRefDisplay() {
    return termBytesRefDisplay;
  }
  
  public BytesRefDisplay getPayloadBytesRefDisplay() {
    return payloadBytesRefDisplay;
  }


  public void registerCommand(ClueCommand cmd){
    String cmdName = cmd.getName();
    if (cmdMap.containsKey(cmdName)){
      throw new IllegalArgumentException(cmdName+" exists!");
    }
    cmdMap.put(cmdName, cmd);
  }
  
  public boolean isInteractiveMode(){
    return interactiveMode;
  }
  
  public boolean isReadOnlyMode() {
    return readOnlyMode;
  }
  
  public ClueCommand getCommand(String cmd){
    return cmdMap.get(cmd);
  }
  
  public Map<String, ClueCommand> getCommandMap(){
    return cmdMap;
  }
  
  public IndexReader getIndexReader(){
    return readerFactory.getIndexReader();
  }
  
  public IndexWriter getIndexWriter(){
    if (readOnlyMode) return null;
    if (writer == null) {
      try {
        writer = new IndexWriter(directory, writerConfig);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return writer;
  }
  
  public Directory getDirectory() {
    return directory;
  }
  
  public void setReadOnlyMode(boolean readOnlyMode) {
    this.readOnlyMode = readOnlyMode;
    if (writer != null) {
      try {
        writer.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      writer = null;
    }
  }
  
  public void refreshReader() throws Exception {
    readerFactory.refreshReader();
  }
  
  public void shutdown() throws Exception{
    try {
      readerFactory.shutdown();
    } 
    finally{
      if (writer != null) {
        writer.close();
      }
    }
  }
}
