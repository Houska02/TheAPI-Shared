package me.devtec.shared.dataholder.loaders;

import java.util.Base64;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import me.devtec.shared.dataholder.Config;
import me.devtec.shared.dataholder.StringContainer;
import me.devtec.shared.dataholder.loaders.constructor.DataValue;
import me.devtec.shared.json.Json;

public class ByteLoader extends EmptyLoader {

	@Override
	public void load(String input) {
		if (input == null || input.length() == 0)
			return;
		reset();
		try {
			byte[] bb = Base64.getDecoder().decode(replace(input));
			ByteArrayDataInput bos = ByteStreams.newDataInput(bb);
			int version = bos.readInt();
			if (version == 3) {
				bos.readInt();
				ByteLoader.byteBuilderV3(this, bos);
			}
			if (!data.isEmpty())
				loaded = true;
		} catch (Exception er) {
			loaded = false;
		}
	}

	@Override
	public byte[] save(Config config, boolean markSaved) {
		try {
			ByteArrayDataOutput in = ByteStreams.newDataOutput();
			in.writeInt(3);
			Iterator<Entry<String, DataValue>> iterator = config.getDataLoader().entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, DataValue> key = iterator.next();
				try {
					if (markSaved)
						key.getValue().modified = false;
					in.writeInt(0);
					in.writeUTF(key.getKey());
					if (key.getValue().value == null) {
						in.writeInt(3);
						continue;
					}
					if (key.getValue().writtenValue != null) {
						String write = key.getValue().writtenValue;
						if (write == null) {
							in.writeInt(3);
							continue;
						}
						while (write.length() > 40000) {
							String wr = write.substring(0, 39999);
							in.writeInt(1);
							in.writeUTF(wr);
							write = write.substring(39999);
						}
						in.writeInt(1);
						in.writeUTF(write);
						continue;
					}
					String write = Json.writer().write(key.getValue().value);
					if (write == null) {
						in.writeInt(3);
						continue;
					}
					while (write.length() > 40000) {
						String wr = write.substring(0, 39999);
						in.writeInt(1);
						in.writeUTF(wr);
						write = write.substring(39999);
					}
					in.writeInt(1);
					in.writeUTF(write);
				} catch (Exception er) {
					er.printStackTrace();
				}
			}
			return in.toByteArray();
		} catch (Exception error) {
			error.printStackTrace();
			return new byte[0];
		}
	}

	@Override
	public String saveAsString(Config config, boolean markSaved) {
		return Base64.getEncoder().encodeToString(save(config, markSaved));
	}

	private static void byteBuilderV3(ByteLoader loader, ByteArrayDataInput bos) {
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
				loader.set(key, DataValue.of(value, Json.reader().read(value)));
				return;
			}
			loader.set(key, DataValue.of(value, Json.reader().read(value)));
			if (result == 0)
				ByteLoader.byteBuilderV3(loader, bos);
		} catch (Exception err) {
		}
	}

	private static String replace(String string) {
		StringContainer builder = new StringContainer(string.length());
		for (int i = 0; i < string.length(); ++i) {
			char c = string.charAt(i);
			if (c == ' ' || c == '	' || c == '\n' || c == '\r')
				continue;
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
				ByteLoader.byteBuilderV3(this, bos);
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
				ByteLoader.byteBuilderV3(loader, bos);
			}
			loader.loaded = true;
		} catch (Exception er) {
			loader.loaded = false;
		}
		return loader;
	}

	@Override
	public String name() {
		return "byte";
	}
}
