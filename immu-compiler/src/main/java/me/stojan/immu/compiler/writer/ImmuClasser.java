package me.stojan.immu.compiler.writer;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import me.stojan.immu.compiler.element.ImmuObjectElement;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import java.io.IOException;

/**
 * Created by vuk on 05/01/17.
 */
public abstract class ImmuClasser {

  protected final ImmuObjectElement element;
  private final ClassName className;

  protected ImmuClasser(ImmuObjectElement element) {
    this.element = element;
    this.className = ClassName.get(element.typeElement());
  }

  public abstract TypeSpec generate(ProcessingEnvironment env);

  public final ClassName className() {
    return className;
  }

  public final ClassName objectClass() {
    return className.peerClass("Immutable" + className.simpleName());
  }

  public final ClassName builderClass() {
    return className.peerClass(className.simpleName() + "Builder");
  }
}

