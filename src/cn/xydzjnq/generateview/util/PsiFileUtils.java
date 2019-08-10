package cn.xydzjnq.generateview.util;

import cn.xydzjnq.generateview.Element;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlRecursiveElementVisitor;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;

import java.util.ArrayList;

public class PsiFileUtils {
    /**
     * @param psiElement
     * @param name
     * @return
     */
    public static PsiFile[] getPsiFiles(PsiElement psiElement, String name) {
        Project project = psiElement.getProject();
        Module module = ModuleUtil.findModuleForPsiElement(psiElement);
        GlobalSearchScope scope = GlobalSearchScope.moduleScope(module);
        PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, name, scope);
        if (psiFiles.length == 0) {
            psiFiles = FilenameIndex.getFilesByName(project, name, new EverythingGlobalScope(project));
        }
        return psiFiles;
    }

    /**
     * @param psiFile
     * @param elementList 为了遍历include标签
     */
    public static void parseElements(PsiFile psiFile, ArrayList<Element> elementList) {
        psiFile.accept(new XmlRecursiveElementVisitor() {
            @Override
            public void visitXmlTag(XmlTag tag) {
                super.visitXmlTag(tag);
                if ("include".equals(tag.getName())) {
                    XmlAttribute attribute = tag.getAttribute("layout");
                    if (attribute == null) {
                        return;
                    }
                    String value = attribute.getValue();
                    if (value.contains("@layout/")) {
                        value = value.substring(8);
                    }
                    String name = String.format("%s.xml", value);
                    PsiFile[] psiFiles = PsiFileUtils.getPsiFiles(psiFile, name);
                    if (psiFiles.length != 0) {
                        PsiFile xmlPsiFile = psiFiles[0];
                        PsiFileUtils.parseElements(xmlPsiFile, elementList);
                    }
                } else {
                    XmlAttribute attribute = tag.getAttribute("android:id");
                    if (attribute == null) {
                        return;
                    }
                    String type = tag.getName();
                    String value = attribute.getValue();
                    if (value.contains("@+id/")) {
                        value = value.substring(5);
                    }
                    Element element = new Element(type, value);
                    elementList.add(element);
                }
            }
        });
    }
}
