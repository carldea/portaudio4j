
import jdk.incubator.foreign.*;
import org.portaudio.PaStreamCallback;
import org.portaudio.foo_h;
import static jdk.incubator.foreign.CLinker.*;
import static org.portaudio.foo_h.*;

/**
 * This is a port of Port Audio's example paex_saw.c.
 *
 * http://portaudio.com/docs/v19-doxydocs/writing_a_callback.html
 *
 * <pre>
 *     WARNING: THIS is extremely loud!!! please have the mute READY or the volume at it's lowest.
 *     Known issues:
 *       1) The Panama version (this file), is too loud and may have some timing issue as
 *          the sound doesn't sound the same as the high pitch of the native C version.
 *       2) This version is for MacOS at the moment.
 *       3) One setting is different. To have the same settings for SAMPLE_RATE
 *          the example would be too loud!!! So, it's currently at 22050 hz.
 *
 * </pre>
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

//    public static int SAMPLE_RATE = 44100;
    public static int SAMPLE_RATE = 22050;
//    public static int SAMPLE_RATE = 11025;
    public static int NUM_SECONDS = 4;
    public static MemoryAddress pStream;
    public static final MemoryLayout paTestData = MemoryLayout.structLayout(
            C_FLOAT.withName("left_phase"),
            C_FLOAT.withName("right_phase")
    ).withName("paTestData");

    private static void error(int err) {
        Pa_Terminate();
        StringBuilder sb = new StringBuilder();
        sb.append("An error occurred while using the portaudio stream\n" )
          .append("Error number: %d\n".formatted( err ))
          .append("Error message: %s\n".formatted(toJavaString(Pa_GetErrorText( err ))));
        throw new RuntimeException(sb.toString());
    }
    public static void main(String[] args) {
//        Thread terminatePortAudio = new Thread(() -> {
//            System.out.println("In the middle of a shutdown");
//            Pa_Terminate();
//        });
//        Runtime.getRuntime().addShutdownHook(terminatePortAudio);

        try (var scope = ResourceScope.newSharedScope()) {
            var allocator = SegmentAllocator.ofScope(scope);
            System.out.printf("PortAudio Test: output sawtooth wave.\n");

            /* Initialize our data for use by callback. */
            // PaStream *stream;
            MemorySegment ppStream = allocator.allocate(C_POINTER);
            var data = allocator.allocate(paTestData.byteSize());

            var vhLeftPhase = paTestData.varHandle(float.class, MemoryLayout.PathElement.groupElement("left_phase"));
            var vhRightPhase = paTestData.varHandle(float.class, MemoryLayout.PathElement.groupElement("right_phase"));
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
                    data.address() );

            if( err != paNoError()) error(err);

            pStream = MemoryAccess.getAddress(ppStream);
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

                var vHLeftPhase = userData.asSegment(4, scope);
                var vHRightPhase = userData.addOffset(4).asSegment(4, scope);

                for(int i=0; i<framesPerBuffer; i++ ) {

//                    *out++ = data->left_phase;  /* left */
//                    *out++ = data->right_phase;  /* right */

                    var out = outputBuffer.addOffset(i * 4).asSegment(4, scope);
                    MemoryAccess.setFloat(out, MemoryAccess.getFloat(vHLeftPhase));
                    out = outputBuffer.addOffset((i * 4) + 4).asSegment(4, scope);
                    MemoryAccess.setFloat(out, MemoryAccess.getFloat(vHRightPhase));

                    MemoryAccess.setFloat(vHLeftPhase, MemoryAccess.getFloat(vHLeftPhase) + 0.01f);
                    if( MemoryAccess.getFloat(vHLeftPhase) >= 1.0f ) {
                        MemoryAccess.setFloat(vHLeftPhase, MemoryAccess.getFloat(vHLeftPhase) - 2.0f);
                    }

                    MemoryAccess.setFloat(vHRightPhase, MemoryAccess.getFloat(vHRightPhase) + 0.03f);
                    if( MemoryAccess.getFloat(vHRightPhase) >= 1.0f ) {
                        MemoryAccess.setFloat(vHRightPhase, MemoryAccess.getFloat(vHRightPhase) - 2.0f);
                    }
                }
                return 0;
            }
        };
    }
}