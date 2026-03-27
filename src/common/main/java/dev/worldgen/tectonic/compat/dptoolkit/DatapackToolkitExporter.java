package dev.worldgen.tectonic.compat.dptoolkit;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import dev.worldgen.tectonic.Tectonic;
import dev.worldgen.tectonic.client.ConfigListBuilder;
import dev.worldgen.tectonic.config.state.object.NoiseState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.GsonHelper;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;

// TODO: Finish this
public class DatapackToolkitExporter implements ConfigListBuilder {
	private static final boolean DISABLED = true;
	
	private final JsonObject meta = new JsonObject();
	private final JsonArray widgets = new JsonArray();
	private final JsonObject methods = new JsonObject();
	
	@Override
	public void addCategory(DisplayMode displayMode, String name, Font font) {
		if (displayMode == DisplayMode.MOD_ONLY) return;
		
		JsonObject object = ofType("heading");
		addText(object, I18n.get(ConfigListBuilder.category(name)));
		this.widgets.add(object);
	}
	
	@Override
	public void addBoolean(DisplayMode displayMode, String name, Consumer<Boolean> setter, boolean getter, boolean defaultValue) {
		if (displayMode == DisplayMode.MOD_ONLY) return;
		// Widget
		JsonObject widget = ofType("switch");
		addText(widget, I18n.get(ConfigListBuilder.option(name)));
		JsonObject value = new JsonObject();
		value.addProperty("default", defaultValue);
		widget.add("value", value);
		widget.addProperty("method", name);
		this.widgets.add(widget);
		
		// Method
		JsonObject method = method();
		this.methods.add(name, method);
	}
	
	@Override
	public void addInteger(DisplayMode displayMode, String name, double min, double max, double step, Consumer<Integer> setter, double getter, double defaultValue) {
		if (displayMode == DisplayMode.MOD_ONLY) return;
	
	}
	
	@Override
	public void addDouble(DisplayMode displayMode, String name, double min, double max, double step, Consumer<Double> setter, double getter, double defaultValue) {
		if (displayMode == DisplayMode.MOD_ONLY) return;
	
	}
	
	@Override
	public void addOverlay(DisplayMode displayMode, String name, Consumer<Boolean> setter, boolean getter, boolean defaultValue) {
		if (displayMode == DisplayMode.MOD_ONLY) return;
		// Widget
		JsonObject widget = ofType("switch");
		addText(widget, I18n.get(ConfigListBuilder.option(name)));
		JsonObject widgetValue = new JsonObject();
		widgetValue.addProperty("default", defaultValue);
		widget.add("value", widgetValue);
		widget.addProperty("method", name);
		this.widgets.add(widget);
		
		// Method
		JsonObject method = method();
		
		JsonObject value = new JsonObject();
		value.addProperty("function", "if_else");
		value.addProperty("argument", true);
		value.addProperty("operator", "==");
		value.addProperty("argument1", "$input");
		value.addProperty("true", name);
		value.addProperty("disabled", name + "_disabled");
		method.add("value", value);
		
		JsonObject accessor = new JsonObject();
		accessor.addProperty("method", "set");
		accessor.addProperty("file_path", "pack.mcmeta");
		String index = name.equals("ultrasmooth") ? "2": "1";
		accessor.addProperty("value_path", "overlays/entries/" + index + "/directory");
		JsonArray array = new JsonArray();
		array.add(accessor);
		method.add("accessors", array);
		
		this.methods.add(name, method);
	}
	
	@Override
	public void addNoise(DisplayMode displayMode, String name, NoiseState state, NoiseState defaultState) {
		if (displayMode == DisplayMode.MOD_ONLY) return;
	
	}
	
	private static JsonObject ofType(String type) {
		JsonObject object = new JsonObject();
		object.addProperty("type", type);
		return object;
	}
	
	private JsonObject method() {
		JsonObject object = new JsonObject();
		object.addProperty("value", "$input");
		return object;
	}
	
	private static void addText(JsonObject object, String text) {
		object.addProperty("text", text);
	}
	
	public static void export() {
		if (DISABLED) return;
		DatapackToolkitExporter exporter = new DatapackToolkitExporter();
		
		exporter.meta.addProperty("ver", 2);
		exporter.meta.addProperty("tab", "Tectonic Config");
		exporter.meta.addProperty("id", "tectonic");
		
		JsonObject title = ofType("title");
		title.addProperty("text", "Tectonic Config");
		exporter.widgets.add(title);
		
		exporter.build(null);
		
		JsonObject root = new JsonObject();
		JsonObject config = new JsonObject();
		config.add("meta", exporter.meta);
		config.add("widgets", exporter.widgets);
		config.add("methods", exporter.methods);
		root.add("config", config);
		
		Path path = Tectonic.FOLDER.resolve("exports");
		path.toFile().mkdirs();
		try (BufferedWriter writer = Files.newBufferedWriter(path.resolve("dpconfig.json"))) {
			StringWriter stringWriter = new StringWriter();
			JsonWriter jsonWriter = new JsonWriter(stringWriter);
			jsonWriter.setIndent("  ");
			GsonHelper.writeValue(jsonWriter, root, Comparator.naturalOrder());
			writer.write(stringWriter.toString());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
