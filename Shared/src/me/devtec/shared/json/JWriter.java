package me.devtec.shared.json;

public interface JWriter {

	public default Object writeWithoutParse(Object s) {
		return JsonUtils.writeWithoutParseStatic(s);
	}

	public default String write(Object s) {
		try {
			if (s == null)
				return "null";
			return s instanceof String || s instanceof CharSequence ? '"' + s.toString() + '"'
					: s instanceof Number || s instanceof Character ? '\'' + s.toString() + '\'' : toGson(writeWithoutParse(s));
		} catch (Exception err) {
		}
		return null;
	}

	// For lists or maps
	public default String simpleWrite(Object object) {
		if (object == null)
			return "null";
		if (object instanceof String || object instanceof CharSequence || object instanceof Boolean || object instanceof Number || object instanceof Character)
			return object.toString();
		return toGson(object);
	}

	public String toGson(Object object);
}
