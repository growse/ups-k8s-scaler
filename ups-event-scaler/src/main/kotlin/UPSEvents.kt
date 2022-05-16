enum class UPSEvents(val cliArg: String) {
    OnLine("online"), OnBattery("onbatt"), LowBattery("lowbatt");

    companion object {
        fun parse(arg: String): UPSEvents? = values().firstOrNull { it.cliArg == arg }
    }
}