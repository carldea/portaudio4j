
import jdk.incubator.foreign.*;
import org.portaudio.PaStreamCallback;
import org.portaudio.foo_h;
import static org.portaudio.foo_h.*;

/**
 * This is a port of Port Audio's example paex_saw.c.
 *
 * http://portaudio.com/docs/v19-doxydocs/writing_a_callback.html
 * To run the Java version:
 * <code>
 *     $ ./jextract_foo.h.sh
 *     $ export DYLD_LIBRARY_PATH=.:$HOME/projects/portaudio/lib/.libs
 *     $ java --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.foreign -classpath .:classes PaexSaw
 * </code>
 *
 * <code>
 *    $ gcc -Wall \
 *       -I ../include \
 *       -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include \
 *       paex_saw.c \
 *       -o paex_saw ../lib/.libs/libportaudio.a \
 *       -framework CoreServices \
 *       -framework CoreFoundation \
 *       -framework AudioUnit \
 *       -framework AudioToolbox \
 *       -framework CoreAudio
 * </code>
 * @author cdea
 */
public class PaexSaw {

    public static int SAMPLE_RATE = 44100;
    public static int NUM_SECONDS = 4;
    public static final MemoryLayout paTestData = MemoryLayout.structLayout(
            C_FLOAT.withName("left_phase"),
            C_FLOAT.withName("right_phase")
    ).withName("paTestData");

    private static void error(int err) {
        Pa_Terminate();
        StringBuilder sb = new StringBuilder();
        String errMsg = Pa_GetErrorText( err ).getUtf8String(0);
        sb.append("An error occurred while using the portaudio stream\n" )
          .append("Error number: %d\n".formatted( err ))
          .append("Error message: %s\n".formatted(errMsg));
        throw new RuntimeException(sb.toString());
    }
    public static void main(String[] args) {

        try (var scope = ResourceScope.newSharedScope()) {
            var allocator = SegmentAllocator.newNativeArena(scope);
            System.out.printf("PortAudio Test: output sawtooth wave.\n");

            /* Initialize our data for use by callback. */
            // PaStream *stream;
            MemorySegment ppStream = MemorySegment.allocateNative(C_POINTER, scope);

            var data = allocator.allocate(paTestData);

            var vhLeftPhase = paTestData.varHandle(MemoryLayout.PathElement.groupElement("left_phase"));
            var vhRightPhase = paTestData.varHandle(MemoryLayout.PathElement.groupElement("right_phase"));
            vhLeftPhase.set(data, 0.0f);
            vhRightPhase.set(data, 0.0f);
            System.out.println("vhLeftPhase.get(data) " + vhLeftPhase.get(data));
            System.out.println("vhRightPhase.get(data) " + vhRightPhase.get(data));

            /* Initialize library before making any other calls. */
            int err = foo_h.Pa_Initialize();
            if( err != paNoError()) error(err);
            PaStreamCallback paStreamCallback = createPaStreamCallback(scope);
            var pPaStreamCallback = PaStreamCallback.allocate(paStreamCallback, scope);

            /* Open an audio I/O stream. */
            err = Pa_OpenDefaultStream(
                    ppStream,
                    0,        /* no input channels */
                    2,       /* stereo output */
                    foo_h.paFloat32(),       /* 32 bit floating point output */
                    SAMPLE_RATE,
                    256,        /* frames per buffer */
                    pPaStreamCallback,
                    data );

            if( err != paNoError()) error(err);

            MemoryAddress pStream = ppStream.get(C_POINTER,0);
            err = Pa_StartStream( pStream );
            if( err != paNoError()) error(err);

            /* Sleep for several seconds. */
            Pa_Sleep(NUM_SECONDS*1000);
            err = Pa_StopStream( pStream );
            if( err != paNoError()) error(err);

            err = Pa_CloseStream( pStream );
            if( err != paNoError()) error(err);

            System.out.println("PortAudio4J");

        } catch (Throwable throwable) {
              System.err.println( throwable.getMessage());
              throwable.printStackTrace();
        } finally {
            Pa_Terminate();
        }

        System.out.printf("Test finished.\n");
    }

    private static PaStreamCallback createPaStreamCallback(ResourceScope scope) {
        return new PaStreamCallback() {
                            /*
             static int patestCallback(
                           const void *inputBuffer,
                           void *outputBuffer,
                           unsigned long framesPerBuffer,
                           const PaStreamCallbackTimeInfo* timeInfo,
                           PaStreamCallbackFlags statusFlags,
                           void *userData
                           )
                             */
            @Override
            public int apply(MemoryAddress inputBuffer,
                             MemoryAddress outputBuffer,
                             long framesPerBuffer,
                             MemoryAddress timeInfo,
                             long statusFlags,
                             MemoryAddress userData) {

                var vHLeftPhase = MemorySegment.ofAddress(userData, 4, scope);
                var vHRightPhase = MemorySegment.ofAddress(vHLeftPhase.address().addOffset(4), 4, scope);
                for(int i=0; i<framesPerBuffer; i++ ) {
                    var out = outputBuffer.addOffset(i * 4);

                    out.set(C_FLOAT, (i*4), vHLeftPhase.get(C_FLOAT, 0));
                    out.set(C_FLOAT, (i*4) + 4, vHRightPhase.get(C_FLOAT, 0));
//                    *out++ = data->left_phase;  /* left */
//                    *out++ = data->right_phase;  /* right */

                    vHLeftPhase.set(C_FLOAT, 0, vHLeftPhase.get(C_FLOAT, 0) + 0.01f);
                    if( vHLeftPhase.get(C_FLOAT, 0) >= 1.0f ) {
                        vHLeftPhase.set(C_FLOAT, 0, vHLeftPhase.get(C_FLOAT, 0) - 2.0f);
                    }

                    vHRightPhase.set(C_FLOAT, 0, vHRightPhase.get(C_FLOAT, 0) + 0.03f);
                    if( vHRightPhase.get(C_FLOAT, 0) >= 1.0f ) {
                        vHRightPhase.set(C_FLOAT, 0, vHRightPhase.get(C_FLOAT, 0) - 2.0f);
                    }
                }
                return 0;
            }
        };
    }
}