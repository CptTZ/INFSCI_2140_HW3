package Search;

import Classes.Query;

public class ExtractQuery {

	public ExtractQuery() {
		//you should extract the 4 queries from the Path.TopicDir
		//NT: the query content of each topic should be 1) tokenized, 2) to lowercase, 3) remove stop words, 4) stemming
		//NT: you can simply pick up title only for query, or you can also use title + description + narrative for the query content.
	}
	
	public boolean hasNext()
	{
		return false;
	}
	
	public Query next()
	{
		return null;
	}
}
