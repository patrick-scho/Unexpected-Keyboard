package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.graphics.drawable.GradientDrawable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EmojiGridView extends GridView
  implements
    GridView.OnItemClickListener,
    GridView.OnItemLongClickListener
{
  public static final int GROUP_LAST_USE = -1;

  private static final String LAST_USE_PREF = "emoji_last_use";
  private static final String SKINTONE_PREF = "emoji_skintone";
  private static final int SKINTONE_POPUP_MAX_NUM_ROWS = 4;
  private static final int SKINTONE_POPUP_NUM_COLS = 6;

  private List<Emoji> _emojiArray;
  private HashMap<Emoji, Integer> _lastUsed;
  // map saving selected skintones
  // mapping from the number of skintones an emoji supports
  // to the selected index in that number
  // to allow saving different skintones for emojis that support a single one
  // and ones supporting multiple ones
  private HashMap<Integer, Integer> _lastSkintone = new HashMap<>();

  /*
   ** TODO: adapt column width and emoji size
   ** TODO: use ArraySet instead of Emoji[]
   */
  public EmojiGridView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    Emoji.init(context.getResources());
    setOnItemClickListener(this);
    setOnItemLongClickListener(this);
    loadLastUsed();
    loadSkintones();
    setEmojiGroup((_lastUsed.size() == 0) ? 0 : GROUP_LAST_USE);
  }

  public void setEmojiGroup(int group)
  {
    _emojiArray = (group == GROUP_LAST_USE) ? getLastEmojis() : Emoji.getEmojisByGroup(group);
    setAdapter(new EmojiViewAdapter(getContext(), _emojiArray, _lastSkintone));
  }

  public void onItemClick(AdapterView<?> parent, View v, int pos, long id)
  {
    Config config = Config.globalConfig();
    Integer used = _lastUsed.get(_emojiArray.get(pos));

    Emoji e = _emojiArray.get(pos);
    _lastUsed.put(e, (used == null) ? 1 : used.intValue() + 1);

    // select skintone if there is one set for the number of variations this emoji supports
    // on zero variations the map is empty and we just use the regular emoji
    Emoji eUp = (
      _lastSkintone.containsKey(e.skintones().size())
      ? _emojiArray.get(pos).skintones().get(_lastSkintone.get(e.skintones().size()))
      : _emojiArray.get(pos));
    config.handler.key_up(eUp.kv(), Pointers.Modifiers.EMPTY);
    saveLastUsed(); // TODO: opti
  }

  public boolean onItemLongClick(AdapterView<?> parent, View v, int pos, long id)
  {
    Emoji e = _emojiArray.get(pos);
    
    if (e.skintones().size() > 0)
    {
      EmojiPopupGridView gridView = new EmojiPopupGridView(v.getContext(), this, e.skintones(), SKINTONE_POPUP_NUM_COLS);
   
      int cols = SKINTONE_POPUP_NUM_COLS;
      int rows = Math.min(SKINTONE_POPUP_MAX_NUM_ROWS, ((e.skintones().size()-1) / SKINTONE_POPUP_NUM_COLS + 1));

      PopupWindow popup = new PopupWindow(
        gridView,
        v.getWidth() * cols + gridView.getHorizontalSpacing() * (cols-1),
        v.getHeight() * rows + gridView.getVerticalSpacing() * (rows-1));

      gridView.setPopup(popup);
      
      TypedArray s = v.getContext().getTheme().obtainStyledAttributes(null, R.styleable.keyboard, 0, 0);
      int colorKey = s.getColor(R.styleable.keyboard_colorKey, 0);

      GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {colorKey, colorKey});
      float radius = s.getDimension(R.styleable.keyboard_keyBorderRadius, 0);
      bg.setCornerRadius(radius);
      popup.setBackgroundDrawable(bg);
      popup.setOutsideTouchable(true);
      popup.showAsDropDown(v);

      return true;
    }
    
    return false;
  }

  private List<Emoji> getLastEmojis()
  {
    List<Emoji> list = new ArrayList<>(_lastUsed.keySet());
    Collections.sort(list, new Comparator<Emoji>()
        {
          public int compare(Emoji a, Emoji b)
          {
            return _lastUsed.get(b) - _lastUsed.get(a);
          }
        });
    return list;
  }

  private void saveLastUsed()
  {
    SharedPreferences.Editor edit;
    try { edit = emojiSharedPreferences().edit(); }
    catch (Exception _e) { return; }
    HashSet<String> set = new HashSet<String>();
    for (Emoji emoji : _lastUsed.keySet())
      set.add(String.valueOf(_lastUsed.get(emoji)) + "-" + emoji.kv().getString());
    edit.putStringSet(LAST_USE_PREF, set);
    edit.apply();
  }

  private void loadLastUsed()
  {
    _lastUsed = new HashMap<Emoji, Integer>();
    SharedPreferences prefs;
    // Storage might not be available (eg. the device is locked), avoid
    // crashing.
    try { prefs = emojiSharedPreferences(); }
    catch (Exception _e) { return; }
    Set<String> lastUseSet = prefs.getStringSet(LAST_USE_PREF, null);
    if (lastUseSet != null)
      for (String emojiData : lastUseSet)
      {
        String[] data = emojiData.split("-", 2);
        Emoji emoji;
        if (data.length != 2)
          continue ;
        emoji = Emoji.getEmojiByString(data[1]);
        if (emoji == null)
          continue ;
        _lastUsed.put(emoji, Integer.valueOf(data[0]));
      }
  }

  private void saveSkintones()
  {
    SharedPreferences.Editor edit;
    try { edit = emojiSharedPreferences().edit(); }
    catch (Exception _e) { return; }
    HashSet<String> set = new HashSet<String>();
    for (Integer key : _lastSkintone.keySet())
      set.add(key.toString() + "-" + _lastSkintone.get(key).toString());
    edit.putStringSet(SKINTONE_PREF, set);
    edit.apply();
  }

  private void loadSkintones()
  {
    _lastSkintone = new HashMap<Integer, Integer>();
    SharedPreferences prefs;
    // Storage might not be available (eg. the device is locked), avoid
    // crashing.
    try { prefs = emojiSharedPreferences(); }
    catch (Exception _e) { return; }
    Set<String> skintoneSet = prefs.getStringSet(SKINTONE_PREF, null);
    if (skintoneSet != null)
      for (String skintoneData : skintoneSet)
      {
        String[] data = skintoneData.split("-", 2);
        _lastSkintone.put(Integer.parseInt(data[0]), Integer.parseInt(data[1]));
      }
  }

  public void setLastSkintone(int skintonesSize, int skintoneIndex)
  {
    _lastSkintone.put(skintonesSize, skintoneIndex);

    // save scroll position
    int i = getFirstVisiblePosition();

    // update adapter with selected skintone(s)
    setAdapter(new EmojiViewAdapter(getContext(), _emojiArray, _lastSkintone));
    
    // restore scroll position
    setSelection(i);
    
    saveSkintones();
  }

  SharedPreferences emojiSharedPreferences()
  {
    return getContext().getSharedPreferences("emoji_last_use", Context.MODE_PRIVATE);
  }

  static class EmojiView extends TextView
  {
    private Emoji _emoji;
    private Integer _skintoneIndex;
    
    public EmojiView(Context context)
    {
      super(context);
    }

    public Emoji getEmoji()
    {
      return _skintoneIndex != null
        ? _emoji.skintones().get(_skintoneIndex)
        : _emoji;
    }

    public void setEmoji(Emoji emoji)
    {
      _emoji = emoji;
      _skintoneIndex = null;

      setText(emoji.kv().getString());
    }

    public void setSkintoneIndex(int skintoneIndex)
    {
      _skintoneIndex = skintoneIndex;
      setText(_emoji.skintones().get(skintoneIndex).kv().getString());
    }
  }

  static class EmojiViewAdapter extends BaseAdapter
  {
    Context _button_context;

    List<Emoji> _emojiArray;
    HashMap<Integer, Integer> _skintoneIndex;

    public EmojiViewAdapter(Context context, List<Emoji> emojiArray, HashMap<Integer, Integer> skintoneIndex)
    {
      _button_context = new ContextThemeWrapper(context, R.style.emojiGridButton);
      _emojiArray = emojiArray;
      _skintoneIndex = skintoneIndex;
    }

    public EmojiViewAdapter(Context context, List<Emoji> emojiArray)
    {
      this(context, emojiArray, new HashMap<>());
    }

    public int getCount()
    {
      if (_emojiArray == null)
        return (0);
      return (_emojiArray.size());
    }

    public Object getItem(int pos)
    {
      return (_emojiArray.get(pos));
    }

    public long getItemId(int pos)
    {
      return (pos);
    }

    public View getView(int pos, View convertView, ViewGroup parent)
    {
      EmojiView view = (EmojiView)convertView;

      if (view == null)
        view = new EmojiView(_button_context);

      Emoji e = _emojiArray.get(pos);
      view.setEmoji(e);
      int skintonesSize = e.skintones().size();
      if (skintonesSize > 0 && _skintoneIndex.containsKey(skintonesSize))
        view.setSkintoneIndex(_skintoneIndex.get(skintonesSize));

      return view;
    }
  }

  public class EmojiPopupGridView extends GridView
    implements GridView.OnItemClickListener
  {
    public static final int GROUP_LAST_USE = -1;

    private static final String LAST_USE_PREF = "emoji_last_use";

    private List<Emoji> _emojiArray;
    private EmojiGridView _emojiGridView;
    private PopupWindow _popup;

    public EmojiPopupGridView(Context context, EmojiGridView emojiGridView, List<Emoji> emojiArray, int numCols)
    {
      super(context);
      
      this._emojiArray = emojiArray;
      this._emojiGridView = emojiGridView;
      
      setOnItemClickListener(this);
      setNumColumns(numCols);
      setAdapter(new EmojiViewAdapter(context, emojiArray));      
    }

    public void setPopup(PopupWindow popup)
    {
      this._popup = popup;
    }

    public void onItemClick(AdapterView<?> parent, View v, int pos, long id)
    {
      Config config = Config.globalConfig();

      Emoji e = _emojiArray.get(pos);
      
      config.handler.key_up(e.kv(), Pointers.Modifiers.EMPTY);

      _emojiGridView.setLastSkintone(_emojiArray.size(), pos);

      if (_popup != null)
        _popup.dismiss();
    }
  }
}
