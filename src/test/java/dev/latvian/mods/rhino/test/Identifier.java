package dev.latvian.mods.rhino.test;

/**
 * @author LatvianModder
 */
public class Identifier implements IdInherit
{
	public final String namespace;
	public final String path;

	public Identifier(String n, String p)
	{
		namespace = n;
		path = p;
	}

	public Identifier(String i)
	{
		int c = i.indexOf(':');

		if (c == -1)
		{
			namespace = "minecraft";
			path = i;
		}
		else
		{
			namespace = i.substring(0, c);
			path = i.substring(c + 1);
		}
	}

	@Override
	public String toString()
	{
		return "Identifier{" +
				"namespace='" + namespace + '\'' +
				", path='" + path + '\'' +
				'}';
	}
}
