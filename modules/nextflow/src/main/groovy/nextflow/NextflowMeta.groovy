package nextflow

import java.text.SimpleDateFormat
import java.util.regex.Pattern

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import nextflow.util.VersionNumber
import static nextflow.extension.Bolts.DATETIME_FORMAT

/**
 * Models nextflow script properties and metadata
 * 
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Singleton(strict = false)
@ToString(includeNames = true)
@EqualsAndHashCode
class NextflowMeta {

    private static final Pattern DSL_DECLARATION = ~/(?m)^\s*(nextflow\.(preview|enable)\.dsl\s*=\s*(\d))\s*(;?\s*)?(;?\/{2}.*)?$/

    private static boolean ignoreWarnDsl2 = System.getenv('NXF_IGNORE_WARN_DSL2')=='true'

    static trait Flags {
        abstract float dsl
        abstract boolean strict
    }

    @Deprecated
    @Slf4j
    static class Preview implements Flags {
        volatile float dsl
        boolean strict
        boolean recursion

        void setDsl( float num ) {
            if( num != 2 && num != 1 )
                throw new IllegalArgumentException("Not a valid DSL version number: $num")
            if( num == 2 && !ignoreWarnDsl2 )
                log.warn1 "DSL 2 PREVIEW MODE IS DEPRECATED - USE THE STABLE VERSION INSTEAD -- Read more at https://www.nextflow.io/docs/latest/dsl2.html#dsl2-migration-notes"
            dsl = num
        }

        void setRecursion(Boolean recurse) {
            if( recurse )
                log.warn "NEXTFLOW RECURSION IS A PREVIEW FEATURE - SYNTAX AND FUNCTIONALITY CAN CHANGE IN FUTURE RELEASE"
            this.recursion = recurse
        }
    }

    static class Features implements Flags {
        volatile float dsl
        boolean strict
    }

    final VersionNumber version
    final int build

    /*
     * Timestamp as dd-MM-yyyy HH:mm UTC formatted string
     */
    final String timestamp

    @Deprecated
    final Preview preview = new Preview()

    final Features enable = new Features()

    private NextflowMeta() {
        version = new VersionNumber(Const.APP_VER)
        build = Const.APP_BUILDNUM
        timestamp = Const.APP_TIMESTAMP_UTC
    }

    protected NextflowMeta(String ver, int build, String timestamp ) {
        this.version = new VersionNumber(ver)
        this.build = build
        this.timestamp = timestamp
    }

    Map featuresMap() {
        final result = new LinkedHashMap()
        if( isDsl2() )
            result.dsl = 2i
        if( isStrictModeEnabled() )
            result.strict = true
        return result
    }

    Map toJsonMap() {
        final result = new LinkedHashMap<>(5)
        result.version = version.toString()
        result.build = build
        result.timestamp = parseDateStr(timestamp)
        if( isDsl2Final() ) {
            result.enable = featuresMap()
        }
        else if( isDsl2() ) {
            result.preview = featuresMap()
        }
        return result
    }

    private Date parseDateStr(String str) {
        def fmt = new SimpleDateFormat(DATETIME_FORMAT + ' Z')
        fmt.parse(str)
    }

    /**
     * Determine if the workflow script uses DSL2 mode
     * 
     * {@code true} when the workflow script uses DSL2 syntax, {@code false} otherwise.
     */
    boolean isDsl2() {
        enable.dsl == 2
    }

    /**
     * As of the removal of DSL2 preview mode, the semantic of this method
     * is identical to {@link #isDsl2()}.
     * @return
     *  {@code true} when the workflow script uses DSL2 syntax, {@code false} otherwise.
     */
    @Deprecated
    boolean isDsl2Final() {
        enable.dsl == 2
    }

    void enableDsl2() {
        this.enable.dsl = 2
    }

    void disableDsl2() {
        enable.dsl = 1
    }

    boolean isStrictModeEnabled() {
        return enable.strict
    }

    void checkDsl2Mode(String script) {
        final matcher = DSL_DECLARATION.matcher(script)
        final mode = matcher.find() ? matcher.group(2) : null
        if( !mode )
            return
        final ver = matcher.group(3)
        if( mode == 'enable' ) {
            if( ver=='2' )
                enableDsl2()
            else if( ver=='1' )
                disableDsl2()
            else
                throw new IllegalArgumentException("Unknown nextflow DSL version: ${ver}")
        }
        else if( mode == 'preview' )
            throw new IllegalArgumentException("Preview nextflow mode 'preview' is not supported anymore -- Please use `nextflow.enable.dsl=2` instead")
        else
            throw new IllegalArgumentException("Unknown nextflow mode=${matcher.group(1)}")
    }
}
