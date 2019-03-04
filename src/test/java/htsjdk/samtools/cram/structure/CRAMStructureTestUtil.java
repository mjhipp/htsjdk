package htsjdk.samtools.cram.structure;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.cram.build.ContainerFactory;
import htsjdk.samtools.cram.ref.ReferenceContext;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CRAMStructureTestUtil {
    public static final int READ_LENGTH_FOR_TEST_RECORDS = 123;

    private static final SAMFileHeader header = initializeSAMFileHeaderForTests();

    private static SAMFileHeader initializeSAMFileHeaderForTests() {
        final SAMFileHeader header = new SAMFileHeader();

        // arbitrary names and length.  Just ensure we have 10 different valid refs.

        header.addSequence(new SAMSequenceRecord("0", 10));
        header.addSequence(new SAMSequenceRecord("1", 10));
        header.addSequence(new SAMSequenceRecord("2", 10));
        header.addSequence(new SAMSequenceRecord("3", 10));
        header.addSequence(new SAMSequenceRecord("4", 10));
        header.addSequence(new SAMSequenceRecord("5", 10));
        header.addSequence(new SAMSequenceRecord("6", 10));
        header.addSequence(new SAMSequenceRecord("7", 10));
        header.addSequence(new SAMSequenceRecord("8", 10));
        header.addSequence(new SAMSequenceRecord("9", 10));

        return header;
    }

    public static SAMFileHeader getSAMFileHeaderForTests() {
        return header;
    }

    public static CramCompressionRecord createMappedRecord(final int index,
                                                           final int sequenceId,
                                                           final int alignmentStart) {
        final CramCompressionRecord record = new CramCompressionRecord();
        record.index = index;
        record.sequenceId = sequenceId;
        record.alignmentStart = alignmentStart;
        record.setSegmentUnmapped(false);

        record.readBases = "AAA".getBytes();
        record.qualityScores = "!!!".getBytes();
        record.readLength = READ_LENGTH_FOR_TEST_RECORDS;
        record.readName = "A READ NAME";
        record.setLastSegment(true);
        record.readFeatures = Collections.emptyList();
        return record;
    }

    public static CramCompressionRecord createUnmappedRecord(final int index) {
        final CramCompressionRecord record = createMappedRecord(index, SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX, SAMRecord.NO_ALIGNMENT_START);
        record.setSegmentUnmapped(true);
        return record;
    }

    public static List<CramCompressionRecord> getSingleRefRecords(final int recordCount,
                                                                  final int singleSequenceId) {
        final List<CramCompressionRecord> records = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            final CramCompressionRecord record = createMappedRecord(i, singleSequenceId, i + 1);

            // set half unmapped-but-placed, to show that it does not make a difference
            if (i % 2 == 0) {
                record.setSegmentUnmapped(true);
            }

            records.add(record);
        }
        return records;
    }

    public static List<CramCompressionRecord> getMultiRefRecords(final int recordCount) {
        final List<CramCompressionRecord> records = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            final CramCompressionRecord record = createMappedRecord(i, i, i + 1);

            // set half unmapped-but-placed, to show that it does not make a difference
            if (i % 2 == 0) {
                record.setSegmentUnmapped(true);
            }

            records.add(record);
        }
        return records;
    }

    public static List<CramCompressionRecord> getUnmappedRecords(final int recordCount) {
        final List<CramCompressionRecord> records = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            final CramCompressionRecord record = createUnmappedRecord(i);
            records.add(record);
        }
        return records;
    }


    // these two sets of records are "half" unplaced: they have either a valid reference index or start position,
    // but not both.  We treat these weird edge cases as unplaced.

    public static List<CramCompressionRecord> getHalfUnmappedNoRefRecords(final int recordCount) {
        final List<CramCompressionRecord> records = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            final CramCompressionRecord record = createMappedRecord(i, SAMRecord.NO_ALIGNMENT_REFERENCE_INDEX, i + 1);
            record.setSegmentUnmapped(true);
            records.add(record);
        }
        return records;
    }

    public static List<CramCompressionRecord> getHalfUnmappedNoStartRecords(final int recordCount, final int sequenceId) {
        final List<CramCompressionRecord> records = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            final CramCompressionRecord record = createMappedRecord(i, sequenceId, SAMRecord.NO_ALIGNMENT_START);
            record.setSegmentUnmapped(true);
            records.add(record);
        }
        return records;
    }

    public static List<CramCompressionRecord> getSingleRefRecordsWithOneUnmapped(final int testRecordCount, final int mappedSequenceId) {
        final List<CramCompressionRecord> retval = getSingleRefRecords(testRecordCount - 1, mappedSequenceId);
        retval.add(createUnmappedRecord(testRecordCount - 1));
        return retval;
    }

    public static List<CramCompressionRecord> getMultiRefRecordsWithOneUnmapped(final int testRecordCount) {
        final List<CramCompressionRecord> retval = getMultiRefRecords(testRecordCount - 1);
        retval.add(createUnmappedRecord(testRecordCount - 1));
        return retval;
    }

    public static List<Container> getMultiRefContainersForStateTest() {
        final ContainerFactory factory = new ContainerFactory(getSAMFileHeaderForTests(), 10);
        final List<Container> testContainers = new ArrayList<>(3);

        final List<CramCompressionRecord> records = new ArrayList<>();

        int index = 0;
        records.add(createMappedRecord(index, index, index + 1));
        final Container container0 = factory.buildContainer(records);

        index++;
        records.add(createMappedRecord(index, index, index + 1));
        final Container container1 = factory.buildContainer(records);

        index++;
        records.add(createUnmappedRecord(index));
        final Container container2 = factory.buildContainer(records);

        testContainers.add(container0);
        testContainers.add(container1);
        testContainers.add(container2);
        return testContainers;
    }

    // assert that slices and containers have values equal to what the caller expects

    public static void assertSliceState(final Slice slice,
                                        final ReferenceContext expectedReferenceContext,
                                        final int expectedAlignmentStart,
                                        final int expectedAlignmentSpan,
                                        final int expectedRecordCount,
                                        final int expectedBaseCount) {
        Assert.assertEquals(slice.getReferenceContext(), expectedReferenceContext);
        Assert.assertEquals(slice.alignmentStart, expectedAlignmentStart);
        Assert.assertEquals(slice.alignmentSpan, expectedAlignmentSpan);
        Assert.assertEquals(slice.nofRecords, expectedRecordCount);
        Assert.assertEquals(slice.bases, expectedBaseCount);
    }

    public static void assertSliceState(final Slice slice,
                                        final ReferenceContext expectedReferenceContext,
                                        final int expectedAlignmentStart,
                                        final int expectedAlignmentSpan,
                                        final int expectedRecordCount,
                                        final int expectedBaseCount,
                                        final int expectedGlobalRecordCounter) {
        assertSliceState(slice, expectedReferenceContext, expectedAlignmentStart, expectedAlignmentSpan, expectedRecordCount, expectedBaseCount);
        Assert.assertEquals(slice.globalRecordCounter, expectedGlobalRecordCounter);
    }

    public static void assertContainerState(final Container container,
                                            final ReferenceContext expectedReferenceContext,
                                            final int expectedAlignmentStart,
                                            final int expectedAlignmentSpan) {
        Assert.assertEquals(container.getReferenceContext(), expectedReferenceContext);
        Assert.assertEquals(container.alignmentStart, expectedAlignmentStart);
        Assert.assertEquals(container.alignmentSpan, expectedAlignmentSpan);
    }

    public static void assertContainerState(final Container container,
                                            final ReferenceContext expectedReferenceContext,
                                            final int expectedAlignmentStart,
                                            final int expectedAlignmentSpan,
                                            final int expectedRecordCount,
                                            final int expectedBaseCount,
                                            final int expectedGlobalRecordCounter) {
        assertContainerState(container, expectedReferenceContext, expectedAlignmentStart, expectedAlignmentSpan);

        Assert.assertEquals(container.nofRecords, expectedRecordCount);
        Assert.assertEquals(container.bases, expectedBaseCount);
        Assert.assertEquals(container.globalRecordCounter, expectedGlobalRecordCounter);

        Assert.assertEquals(container.slices.length, 1);

        // verify the underlying slice too

        assertSliceState(container.slices[0], expectedReferenceContext, expectedAlignmentStart, expectedAlignmentSpan,
                expectedRecordCount, expectedBaseCount, expectedGlobalRecordCounter);
    }
}