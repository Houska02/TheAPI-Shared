package me.devtec.shared.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.devtec.shared.Ref;
import me.devtec.shared.components.ClickEvent.Action;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.utility.StringUtils;

public class ComponentAPI {
	static Pattern url = Pattern.compile("(w{3}\\.|[a-zA-Z0-9+&@#/%?=~_|!:,.;-]+:\\/\\/)?[a-zA-Z0-9+&@#/%?=~_|!:,.;-]+\\w\\.[a-zA-Z0-9+&@#/%?=~_|!:,.;-]{1,}\\w");
	static Map<String, ComponentTransformer<?>> transformers = new HashMap<>();

	public static ComponentTransformer<?> transformer(String name) {
		return ComponentAPI.transformers.get(name.toUpperCase());
	}

	public static ComponentTransformer<?> registerTransformer(String name, ComponentTransformer<?> transformer) {
		if (ComponentAPI.transformers.put(name.toUpperCase(), transformer) != null)
			Logger.getGlobal().warning("[TheAPI/ComponentAPI] Overriding " + name.toUpperCase() + " component transformer.");
		return transformer;
	}

	public static ComponentTransformer<?> unregisterTransformer(String name) {
		return ComponentAPI.transformers.remove(name.toUpperCase());
	}

	public static ComponentTransformer<?> bungee() {
		return ComponentAPI.transformer("BUNGEECORD");
	}

	public static ComponentTransformer<?> adventure() {
		return ComponentAPI.transformer("ADVENTURE");
	}

	public static String toString(Component input) {
		if (input == null)
			return null;
		return input.toString(); // Are you lazy or stupid?
	}

	public static Component fromString(String input) {
		if (input == null)
			return null;
		return ComponentAPI.fromString(input, /* Depends on version & software */ !Ref.serverType().isBukkit() || Ref.serverType().isBukkit() && Ref.isNewerThan(15), input.contains("."));
	}

	public static Component fromString(String input, boolean hexMode) {
		if (input == null)
			return null;
		return ComponentAPI.fromString(input, hexMode ? !Ref.serverType().isBukkit() || Ref.serverType().isBukkit() && Ref.isNewerThan(15) : false, input.contains("."));
	}

	public static Component fromString(String inputText, boolean hexMode, boolean urlMode) {
		if (inputText == null)
			return null;
		final Component start = new Component("");
		if (inputText.isEmpty())
			return start;

		final List<Component> extra = new ArrayList<>();

		// REQUIRES hexMode ENABLED
		String hex = null;
		char prev = 0;

		String[] splits = inputText.split("\n");
		boolean onEnd = inputText.endsWith("\n");
		int splitPos = 0;

		for (String input : splits) {
			Component current = new Component();
			extra.add(current);
			if (splits.length == 1 && onEnd || splits.length > 1 && (++splitPos < splits.length || splitPos == splits.length && onEnd))
				input = input + "\n";

			StringContainer builder = new StringContainer(input.length());

			for (int i = 0; i < input.length(); ++i) {
				char c = input.charAt(i);
				// COLOR or FORMAT
				if (prev == '§') {
					prev = c;

					if (hexMode && c == 'x') {
						builder.deleteCharAt(builder.length() - 1); // Remove §
						hex = "#";
						continue;
					}

					// COLOR
					if (c >= 97 && c <= 102 || c >= 48 && c <= 57) {
						if (hex != null) {
							hex += c;
							builder.deleteCharAt(builder.length() - 1); // Remove §
							if (hex.length() == 7) {
								current.setText(builder.toString()); // Current builder into text
								builder.clear(); // Clear builder
								current = new Component(); // Create new component
								extra.add(current);
								current.setColor(hex); // Set current format component to bold
								hex = null; // reset hex
							}
							continue;
						}
						builder.deleteCharAt(builder.length() - 1); // Remove §
						current.setText(builder.toString()); // Current builder into text
						if (current.getText().trim().isEmpty()) { // Just space or empty Component.. fast fix
							current.setColor(null);
							current.setFormatFromChar('r', false);
						}
						builder.clear(); // Clear builder
						current = new Component(); // Create new component
						extra.add(current);
						current.setColorFromChar(c);
						continue;
					}
					// FORMAT
					if (c >= 107 && c <= 111 || c == 114) {
						hex = null;
						builder.deleteCharAt(builder.length() - 1); // Remove §
						current.setText(builder.toString()); // Current builder into text
						builder.clear(); // Clear builder
						current = new Component().copyOf(current); // Create new component
						extra.add(current);
						current.setFormatFromChar(c, c != 114); // Set current format to 'true' or reset all
						continue;
					}
				}
				if (urlMode && c == ' ' && builder.indexOf('.') != -1) {
					String text = builder.toString();
					Matcher urlFinder = ComponentAPI.checkHttp(text);
					if (urlFinder != null) {
						// Before url
						current.setText(text.substring(0, urlFinder.start()));
						builder.delete(0, urlFinder.start());

						// url
						text = builder.toString();

						// clear
						builder.clear();

						// url
						current = new Component(text).copyOf(current);
						extra.add(current);
						current.setClickEvent(new ClickEvent(Action.OPEN_URL, text.startsWith("https://") || text.startsWith("http://") ? text : "https://" + text));

						current = new Component().copyOf(current);
						extra.add(current);
					}
				}
				prev = c;
				builder.append(c);
			}

			String text = builder.toString();
			if (urlMode && builder.indexOf('.') != -1) {
				Matcher urlFinder = ComponentAPI.checkHttp(text);
				if (urlFinder != null) {
					// Before url
					current.setText(text.substring(0, urlFinder.start()));
					builder.delete(0, urlFinder.start());

					// url
					text = builder.toString();

					// clear
					builder.clear();

					// url
					current = new Component(text).copyOf(current);
					extra.add(current);
					current.setClickEvent(new ClickEvent(Action.OPEN_URL, text.startsWith("https://") || text.startsWith("http://") ? text : "https://" + text));
				} else
					current.setText(text);
			} else
				current.setText(text);
			hex = null;
			prev = 0;
		}

		filterInvalid(extra);
		start.setExtra(extra);
		return start;
	}

	private static void filterInvalid(List<Component> extra) {
		ListIterator<Component> itr = extra.listIterator();
		while (itr.hasNext()) {
			Component next = itr.next();
			if (next.getText() == null || next.getText().isEmpty())
				itr.remove();
		}
	}

	private static Matcher checkHttp(String text) {
		Matcher m = ComponentAPI.url.matcher(text);
		return m.find() ? m : null;
	}

	public static List<Map<String, Object>> toJsonList(Component component) {
		List<Map<String, Object>> list = new LinkedList<>();
		list.add(component.toJsonMap());
		if (component.getExtra() != null)
			ComponentAPI.toJsonListAll(list, component.getExtra());
		return list;
	}

	private static void toJsonListAll(List<Map<String, Object>> list, List<Component> extra) {
		for (Component c : extra) {
			list.add(c.toJsonMap());
			if (c.getExtra() != null)
				ComponentAPI.toJsonListAll(list, c.getExtra());
		}
	}

	public static List<Map<String, Object>> toJsonList(String text) {
		return ComponentAPI.toJsonList(ComponentAPI.fromString(text));
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> fixJsonList(List<Map<String, Object>> lists) { // usable for ex. chat format
		if (lists == null || lists.isEmpty())
			return lists;
		List<Map<String, Object>> list = new ArrayList<>();

		Iterator<Map<String, Object>> it = lists.listIterator();
		while (it.hasNext()) {
			Map<String, Object> text = it.next();

			if (text.get("text") == null || text.get("text").toString().isEmpty())
				continue; // fast skip

			Map<String, Object> hover = convertMapValues("hoverEvent", (Map<String, Object>) text.get("hoverEvent"));
			Map<String, Object> click = convertMapValues("clickEvent", (Map<String, Object>) text.get("clickEvent"));
			String insertion = (String) text.get("insertion");

			String stext = (String) text.get("text");
			Component c = ComponentAPI.fromString(stext);
			if (c != null) {
				if (!c.getText().isEmpty()) {
					Map<String, Object> json = c.toJsonMap();
					if (hover != null)
						json.put("hoverEvent", hover);
					if (click != null && c.getClickEvent() == null) // Propably URL
						json.put("clickEvent", click);
					if (insertion != null)
						json.put("insertion", insertion);
					list.add(json);
				}
				if (c.getExtra() != null)
					for (Component extras : c.getExtra())
						addExtras(extras, list, hover, click, insertion);
			}
			Object extra = text.get("extra");
			if (extra != null) {
				List<Map<String, Object>> extras = new ArrayList<>();
				if (extra instanceof Map)
					extras.addAll(fixJsonList(Arrays.asList((Map<String, Object>) extra)));
				else if (extra instanceof List)
					extras.addAll(fixJsonList((List<Map<String, Object>>) extra));
				list.get(list.size() - 1).put("extra", extras);
			}
		}
		return list;
	}

	private static void addExtras(Component extras, List<Map<String, Object>> list, Map<String, Object> hover, Map<String, Object> click, String insertion) {
		Map<String, Object> jsons = extras.toJsonMap();
		if (hover != null)
			jsons.put("hoverEvent", hover);
		if (click != null && extras.getClickEvent() == null) // Propably URL
			jsons.put("clickEvent", click);
		if (insertion != null)
			jsons.put("insertion", insertion);
		list.add(jsons);
		if (extras.getExtra() != null)
			for (Component c : extras.getExtra())
				addExtras(c, list, hover, click, insertion);
	}

	@SuppressWarnings("unchecked")
	public static String listToString(List<?> list) {
		StringContainer builder = new StringContainer(list.size() * 20);
		for (Object text : list)
			if (text instanceof Map)
				builder.append(ComponentAPI.getColor(((Map<String, Object>) text).get("color"))).append(String.valueOf(((Map<String, Object>) text).get("text")));
			else
				builder.append(StringUtils.colorize(text + ""));
		return builder.toString();
	}

	private static String getColor(Object color) {
		if (color == null)
			return "";
		if (color.toString().startsWith("#"))
			return StringUtils.color.replaceHex(color.toString());
		return "§" + Component.colorToChar(color.toString());
	}

	private static Map<String, Object> convertMapValues(String key, Map<String, Object> hover) {
		if (hover == null || hover.isEmpty())
			return null;
		Object val = hover.getOrDefault("value", hover.getOrDefault("content", hover.getOrDefault("contents", null)));
		if (val == null)
			hover.put("value", "");
		else if (key.equalsIgnoreCase("hoverEvent")) {
			if (val instanceof Collection || val instanceof Map) {
				Object ac = hover.get("action");
				hover.clear();
				hover.put("action", ac);
				hover.put("value", val);
			} else {
				Object ac = hover.get("action");
				hover.clear();
				hover.put("action", ac);
				hover.put("value", ComponentAPI.toJsonList(val + ""));
			}
		} else if (val instanceof Collection || val instanceof Map) {
			Object ac = hover.get("action");
			hover.clear();
			hover.put("action", ac);
			hover.put("value", val);
		} else {
			Object ac = hover.get("action");
			hover.clear();
			hover.put("action", ac);
			hover.put("value", val + "");
		}
		return hover;
	}
}
