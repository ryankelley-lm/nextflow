
package nextflow.util

import java.util.List
import nextflow.file.FileHolder
import java.util.Map
import groovy.transform.CompileStatic

/**
 *
 */

@CompileStatic
interface CustomFileAttributesProvider {
    Map GetCustomFileAttributes(List<FileHolder> items)
}
