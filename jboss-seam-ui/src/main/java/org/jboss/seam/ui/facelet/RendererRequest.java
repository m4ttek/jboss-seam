package org.jboss.seam.ui.facelet;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.URL;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.Facelet;
import javax.servlet.ServletContext;

import org.apache.myfaces.view.facelets.FaceletFactory;
import org.apache.myfaces.view.facelets.FaceletViewDeclarationLanguage;
import org.apache.myfaces.view.facelets.impl.DefaultFaceletFactory;
import org.jboss.seam.core.ResourceLoader;
import org.jboss.seam.jsf.DelegatingFacesContext;
import org.jboss.seam.log.LogProvider;
import org.jboss.seam.log.Logging;
import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.jboss.seam.mock.MockHttpServletRequest;
import org.jboss.seam.mock.MockHttpServletResponse;
import org.jboss.seam.ui.util.JSF;




public class RendererRequest
{
   private static final LogProvider log = Logging.getLogProvider(RendererRequest.class);

   private FacesContext originalFacesContext;
   private FacesContext facesContext;

   private MockHttpServletRequest request;
   private EnhancedMockHttpServletResponse response;

   private StringWriter writer;

   private String viewId;

   private ClassLoader originalClassLoader;

   public RendererRequest(String viewId)
   {
      this.viewId = viewId;
   }

   private void init()
   {
      if (FacesContext.getCurrentInstance() != null) {
         request = new MockHttpServletRequest(HttpSessionManager.instance(), FacesContext.getCurrentInstance().getExternalContext());  
      } else {
         request = new MockHttpServletRequest(HttpSessionManager.instance());
      }
      response = new EnhancedMockHttpServletResponse();
      response.setCharacterEncoding("UTF-8");
      setContextClassLoader();

      // Generate the FacesContext from the JSF FacesContextFactory
      originalFacesContext = FacesContext.getCurrentInstance();
      facesContext = RendererFacesContextFactory.instance().getFacesContext(request, response);
      DelegatingFacesContext.setCurrentInstance(facesContext);

      // Create the viewRoot
      UIViewRoot newRoot = facesContext.getApplication().getViewHandler().createView(facesContext, viewId);
      facesContext.setViewRoot(newRoot);

      // Set the responseWriter to write to a buffer
      writer = new StringWriter();
      facesContext.setResponseWriter(facesContext.getRenderKit().createResponseWriter(writer, null, "UTF-8"));
   }

   private void cleanup()
   {
      facesContext.release();
      DelegatingFacesContext.setCurrentInstance(originalFacesContext);

      originalFacesContext = null;
      facesContext = null;
      request = null;
      response = null;
   }

   protected void setContextClassLoader() {
       // JBSEAM-3555 Quick fix
       // Set the context classloader to the cached one
       originalClassLoader = Thread.currentThread().getContextClassLoader();
       ServletContext ctx = request.getSession().getServletContext();
       WeakReference<ClassLoader> ref = (WeakReference<ClassLoader>)ctx.getAttribute("seam.context.classLoader");
       if (ref == null || ref.get() == null) {
           log.warn("Failed to bootstrap context classloader. Facelets may not work properly from MDBs");
       } else {
           Thread.currentThread().setContextClassLoader(ref.get());
       }    
   }

   protected void resetContextClassLoader() {
       // JBSEAM-3555 Quick fix
       if (originalClassLoader != null) {
           Thread.currentThread().setContextClassLoader(originalClassLoader);
           originalClassLoader = null;
       }
   }
   
   public void run() throws IOException
   {
      try {
          init();
          renderFacelet(facesContext, faceletForViewId(viewId));
      } finally {
          cleanup();
          resetContextClassLoader();
      }      
   }

   public String getOutput()
   {
      return writer.getBuffer().toString();
   }

   /**
    * Get a Facelet for a URL
    */
  /* protected Facelet faceletForViewId(String viewId) throws IOException
   {
      URL url = ResourceLoader.instance().getResource(viewId);
      if (url == null)
      {
         throw new IllegalArgumentException("resource doesn't exist: " + viewId);
      }
      return ApplicationAssociate.getCurrentInstance().getFaceletFactory().getFacelet(FacesContext.getCurrentInstance(),url);
   }*/

   protected Facelet faceletForViewId(String viewId) throws IOException {
      URL url = ResourceLoader.instance().getResource(viewId);
      if (url == null) {
         throw new IllegalArgumentException("resource doesn't exist: " + viewId);
      }

      // OLD CODE tightly coupled to Sun JSF
      // return
      // ApplicationAssociate.getCurrentInstance().getFaceletFactory().getFacelet(url);

      // NEW CODE for MyFaces
      // see
      // http://stackoverflow.com/questions/15813582/how-to-programmatically-instantiate-a-composite-component-or-a-tag-component
      FaceletFactory ff = DefaultFaceletFactory.getInstance();
      if (ff == null) {
         FaceletViewDeclarationLanguage vdl = new FaceletViewDeclarationLanguage(facesContext);

         Method createCompiler = null;
         Method createFaceletFactory = null;
         try {
            createCompiler = FaceletViewDeclarationLanguage.class.getDeclaredMethod("createCompiler",
                    FacesContext.class);
            createFaceletFactory = FaceletViewDeclarationLanguage.class.getDeclaredMethod("createFaceletFactory",
                    FacesContext.class, org.apache.myfaces.view.facelets.compiler.Compiler.class);
            createCompiler.setAccessible(true);
            createFaceletFactory.setAccessible(true);
            org.apache.myfaces.view.facelets.compiler.Compiler compiler = (org.apache.myfaces.view.facelets.compiler.Compiler) createCompiler
                    .invoke(vdl, facesContext);
            ff = (FaceletFactory) createFaceletFactory.invoke(vdl, facesContext, compiler);
         } catch (Exception ex) {
            log.error("Error creating FaceletFactory.", ex);
         }

      }

      Facelet facelet = null;
      try {

         facelet = ff.getFacelet(url);

      } catch (Exception ex) {
         log.error("Error creating facelet for url " + url, ex);
      }

      log.debug("Successfully created facelet for " + url + ": " + facelet);

      return facelet;
   }

   /**
    * Render a Facelet
    */
   protected void renderFacelet(FacesContext facesContext, Facelet facelet) throws IOException
   {
      UIViewRoot root = facesContext.getViewRoot();
      facelet.apply(facesContext, root);
      JSF.renderChildren(facesContext, root);  
   }
}
