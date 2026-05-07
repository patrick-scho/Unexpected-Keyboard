package juloo.keyboard2;

import android.content.res.Resources;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Emoji
{
  private final KeyValue _kv;
  private List<Emoji> _skintones;

  protected Emoji(String bytecode)
  {
    this._kv = new KeyValue(bytecode, KeyValue.Kind.String, 0, 0);
    this._skintones = new ArrayList<>();
  }

  protected Emoji(Emoji e)
  {
    this._kv = e.kv();
    this._skintones = new ArrayList<>(e.skintones());
  }

  public KeyValue kv()
  {
    return _kv;
  }

  public List<Emoji> skintones()
  {
    return _skintones;
  }

  public void addSkintone(Emoji skintone)
  {
    _skintones.add(skintone);
  }


  private final static List<Emoji> _all = new ArrayList<>();
  private final static List<List<Emoji>> _groups = new ArrayList<>();
  private final static HashMap<String, Emoji> _stringMap = new HashMap<>();

  public static void init(Resources res)
  {
    if (!_all.isEmpty())
      return;

    try
    {
      InputStream inputStream = res.openRawResource(R.raw.emojis);
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      String line;

      // Read emoji (until empty line)
      while (!(line = reader.readLine()).isEmpty())
      {
        Emoji e = new Emoji(line);

        _all.add(e);
        _stringMap.put(line, e);
      }

      // Read group indices
      if ((line = reader.readLine()) != null)
      {
        String[] tokens = line.split(" ");
        int last = 0;
        for (int i = 1; i < tokens.length; i++)
        {
          int next = Integer.parseInt(tokens[i]);
          _groups.add(_all.subList(last, next));
          last = next;
        }
        _groups.add(_all.subList(last, _all.size()));
      }

      inputStream = res.openRawResource(R.raw.emojis_skintone_modifiable);
      reader = new BufferedReader(new InputStreamReader(inputStream));

      // Read skintone modifiable emojis
      while ((line = reader.readLine()) != null)
      {
          int baseIndex = Integer.parseInt(line);
          Emoji baseEmoji = _all.get(baseIndex);
          
          baseEmoji.addSkintone(new Emoji(baseEmoji));
          
          while (!(line = reader.readLine()).isEmpty())
          {
            baseEmoji.addSkintone(new Emoji(line));
          }
      }
    }
    catch (IOException e) { Logs.exn("Emoji.init() failed", e); }
  }

  public static int getNumGroups()
  {
    return _groups.size();
  }

  public static List<Emoji> getEmojisByGroup(int groupIndex)
  {
    return _groups.get(groupIndex);
  }

  public static Emoji getEmojiByString(String value)
  {
    return _stringMap.get(value);
  }
}
