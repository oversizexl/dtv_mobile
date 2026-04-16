package dtv.mobile.platform.huya.purelive.tars

class HYPushMessage : TarsStruct {
  var pushType: Int = 0
  var uri: Int = 0
  var msg: ByteArray = ByteArray(0)
  var protocolType: Int = 0

  override fun readFrom(input: TarsInputStream) {
    pushType = input.readInt(tag = 0, required = false, defaultValue = 0)
    uri = input.readInt(tag = 1, required = false, defaultValue = 0)
    msg = input.readByteArray(tag = 2, required = false) ?: ByteArray(0)
    protocolType = input.readInt(tag = 3, required = false, defaultValue = 0)
  }
}

class HYSender : TarsStruct {
  var uid: Int = 0
  var lMid: Int = 0
  var nickName: String = ""
  var gender: Int = 0

  override fun readFrom(input: TarsInputStream) {
    uid = input.readInt(tag = 0, required = false, defaultValue = 0)
    lMid = input.readInt(tag = 1, required = false, defaultValue = 0)
    nickName = input.readString(tag = 2, required = false, defaultValue = "")
    gender = input.readInt(tag = 3, required = false, defaultValue = 0)
  }
}

class HYBulletFormat : TarsStruct {
  var fontColor: Int = 0
  var fontSize: Int = 4
  var textSpeed: Int = 0
  var transitionType: Int = 1

  override fun readFrom(input: TarsInputStream) {
    fontColor = input.readInt(tag = 0, required = false, defaultValue = 0)
    fontSize = input.readInt(tag = 1, required = false, defaultValue = 4)
    textSpeed = input.readInt(tag = 2, required = false, defaultValue = 0)
    transitionType = input.readInt(tag = 3, required = false, defaultValue = 1)
  }
}

class HYMessage : TarsStruct {
  var userInfo: HYSender = HYSender()
  var content: String = ""
  var bulletFormat: HYBulletFormat = HYBulletFormat()

  override fun readFrom(input: TarsInputStream) {
    userInfo = input.readStruct(tag = 0, required = false) { HYSender() }
    content = input.readString(tag = 3, required = false, defaultValue = "")
    bulletFormat = input.readStruct(tag = 6, required = false) { HYBulletFormat() }
  }
}

