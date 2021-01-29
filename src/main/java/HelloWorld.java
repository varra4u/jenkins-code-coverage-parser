import java.lang.Math; 
import java.util.*;
import java.util.concurrent.atomic. *;
import java.nio.*;

public class HelloWorld
{
  public static void main(String[] args)
  {
	  final StringBuffer prev = new StringBuffer("") ;
	  final List<String> l = new ArrayList<>();
	  final Map<String, AtomicInteger> m = new HashMap<>();
	  final String s = "aaaabbbccabc";
	  CharBuffer.wrap(s.toCharArray()).chars().mapToObj(ch -> (char)ch).forEach(e->{
		  if(m.containsKey(String.valueOf(e)))
		  {
			  m.get(String.valueOf(e)).incrementAndGet();
		  }
		  else
		  {
			  if(prev.toString().length()>0)
			  {
				  l.add(prev.toString()+m.get(prev.toString()));
				  m.remove(prev.toString());
				  prev.setLength(0);
			  }
			  prev.append(String.valueOf(e));
			  m.put(String.valueOf(e), new AtomicInteger(1));
		  } 
      });
	  l.add(prev.toString()+m.get(prev.toString()));
	  System.out.println(l);
  }
}