# fs2-jms
[![Build Status](https://travis-ci.com/kiambogo/fs2-jms.svg?branch=master)](https://travis-ci.com/kiambogo/fs2-jms)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.kiambogo/fs2-jms_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.kiambogo/fs2-jms_2.12)
[![Coverage Status](https://coveralls.io/repos/github/kiambogo/fs2-jms/badge.svg?branch=master)](https://coveralls.io/github/kiambogo/fs2-jms?branch=master)

[fs2](https://github.com/functional-streams-for-scala/fs2) Streaming utilities for JMS providers

### Supported JMS Message Types

- [x] TextMessage
- [ ] ByteMessage
- [ ] MapMessage
- [ ] ObjectMessage

## Publisher
Example JMS publisher
```scala
val producerSettings = {
  JmsProducerSettings(
    connectionFactory,
    sessionCount = 1,
    queueName = "testQueue"
  )
}

fs2.Stream
  .range(1, 10)
  .map(_.toString)
  .through(textPipe[IO](producerSettings))
```
