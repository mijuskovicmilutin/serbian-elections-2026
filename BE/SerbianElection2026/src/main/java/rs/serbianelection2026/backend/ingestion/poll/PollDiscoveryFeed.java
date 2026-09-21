package rs.serbianelection2026.backend.ingestion.poll;

import java.util.List;
import rs.serbianelection2026.backend.ingestion.entity.ImportSource;
import rs.serbianelection2026.backend.ingestion.poll.dto.DiscoveredItem;

/** A place to look for new polls: a pollster's own feed or the media feeds. Each one is audited under its own source. */
public interface PollDiscoveryFeed {

    ImportSource source();

    List<DiscoveredItem> fetch();
}
