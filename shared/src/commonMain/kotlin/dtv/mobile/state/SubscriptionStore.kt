package dtv.mobile.state

import dtv.mobile.model.Streamer

interface SubscriptionStore {
  fun loadFollowedStreamers(): List<Streamer>
  fun saveFollowedStreamers(items: List<Streamer>)

  fun loadSubscribedPartitions(): List<SubscribedPartition>
  fun saveSubscribedPartitions(items: List<SubscribedPartition>)
}

object InMemorySubscriptionStore : SubscriptionStore {
  private var followed: List<Streamer> = emptyList()
  private var partitions: List<SubscribedPartition> = emptyList()

  override fun loadFollowedStreamers(): List<Streamer> = followed

  override fun saveFollowedStreamers(items: List<Streamer>) {
    followed = items.toList()
  }

  override fun loadSubscribedPartitions(): List<SubscribedPartition> = partitions

  override fun saveSubscribedPartitions(items: List<SubscribedPartition>) {
    partitions = items.toList()
  }
}

