package ivonhoe.gradle.mine;

import com.android.build.gradle.BaseExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * @author Ivonhoe on 11/7/21.
 * @email yangfan3687@163.com
 */
public class MinePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getExtensions().findByType(BaseExtension.class)
                .registerTransform(new MineTransform(project));
    }
}
