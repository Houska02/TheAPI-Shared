package me.devtec.shared.dataholder.loaders;

import java.util.Base64;
import java.util.Map;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.dataholder.loaders.constructor.DataValue;
import me.devtec.shared.json.Json;

public class ByteLoader extends EmptyLoader {

	static char[] separator = System.lineSeparator().toCharArray();

	private static void byteBuilderV3(ByteArrayDataInput bos, Map<String, DataValue> map) {
		try {
			String key = bos.readUTF();
			String value = null;
			int result;
			try {
				while ((result = bos.readInt()) == 1)
					if (value == null)
						value = bos.readUTF();
					else
						value += bos.readUTF();
				if (result == 3) { // null pointer
					value = null;
					result = bos.readInt();
				}
			} catch (Exception err) {
				value = YamlLoader.r(value);
				map.put(key, DataValue.of(value, Json.reader().read(value), null));
				return;
			}
			value = YamlLoader.r(value);
			map.put(key, DataValue.of(value, Json.reader().read(value), null));
			if (result == 0)
				ByteLoader.byteBuilderV3(bos, map);
		} catch (Exception err) {
		}
	}

	@Override
	public void load(String input) {
		reset();
		if (input == null)
			return;
		try {
			byte[] bb = Base64.getDecoder().decode(replace(input));
			ByteArrayDataInput bos = ByteStreams.newDataInput(bb);
			int version = bos.readInt();
			if (version == 3) {
				bos.readInt();
				ByteLoader.byteBuilderV3(bos, data);
			}
			if (!data.isEmpty())
				loaded = true;
		} catch (Exception er) {
			loaded = false;
		}
	}

	private static String replace(String string) {
		StringContainer builder = new StringContainer(string.length());
		int pos = 0;
		for (int i = 0; i < string.length(); ++i) {
			char c = string.charAt(i);
			if (c == ' ' || c == '	')
				continue;
			if (separator[pos] == c) {
				if (++pos == separator.length) {
					builder.delete(builder.length() - separator.length + 1, builder.length());
					pos = 0;
					continue;
				}
			} else
				pos = 0;
			builder.append(c);
		}
		return builder.toString();
	}

	public void load(byte[] byteData) {
		reset();
		if (byteData == null)
			return;
		try {
			ByteArrayDataInput bos = ByteStreams.newDataInput(byteData);
			int version = bos.readInt();
			if (version == 3) {
				bos.readInt();
				ByteLoader.byteBuilderV3(bos, data);
			}
			if (!data.isEmpty())
				loaded = true;
		} catch (Exception er) {
			loaded = false;
		}
	}

	public static ByteLoader fromBytes(byte[] byteData) {
		if (byteData == null)
			return null;
		ByteLoader loader = new ByteLoader();
		try {
			ByteArrayDataInput bos = ByteStreams.newDataInput(byteData);
			int version = bos.readInt();
			if (version == 3) {
				bos.readInt();
				ByteLoader.byteBuilderV3(bos, loader.data);
			}
			loader.loaded = true;
		} catch (Exception er) {
			loader.loaded = false;
		}
		return loader;
	}
}
