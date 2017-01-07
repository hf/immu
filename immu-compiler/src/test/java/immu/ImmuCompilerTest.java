package immu;

import com.squareup.javapoet.JavaFile;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;
import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ImmuCompilerTest {

  private ImmuCompiler compiler;

  @Before
  public void setUp() {
    compiler = new ImmuCompiler();
  }

  @Test
  public void getSupportedOptions() throws Exception {
    assertTrue("Supported options is empty", compiler.getSupportedOptions().isEmpty());
  }

  @Test
  public void getSupportedAnnotationTypes() throws Exception {
    assertTrue("@Immu is supported", compiler.getSupportedAnnotationTypes().contains("immu.Immu"));
    assertTrue("@SuperImmu is supported", compiler.getSupportedAnnotationTypes().contains("immu.Immu"));
  }

  @Test
  public void getSupportedSourceVersion() throws Exception {
    assertTrue("Latest version is supported", SourceVersion.latestSupported().equals(compiler.getSupportedSourceVersion()));
  }

  @Test
  public void getCompletions() throws Exception {
    // just exercise the code
    compiler.getCompletions(null, null, null, null);
  }

  @Test
  public void write_withIOException() throws Exception {
    final ProcessingEnvironment env = mock(ProcessingEnvironment.class);
    final Messager messager = mock(Messager.class);
    final Filer filer = mock(Filer.class);

    when(env.getMessager()).thenReturn(messager);
    when(env.getFiler()).thenReturn(filer);

    compiler.init(env);

    final JavaFile javaFile = mock(JavaFile.class);
    doThrow(IOException.class).when(javaFile).writeTo(any(Filer.class));

    compiler.writeSource(javaFile);

    verify(messager, times(1)).printMessage(eq(Diagnostic.Kind.ERROR), any());
  }


}
