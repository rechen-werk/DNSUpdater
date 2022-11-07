package rechenwerk

import org.openqa.selenium.By.ByXPath
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import java.lang.Exception
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Date
private val options = FirefoxOptions()
private val folder: Path = Path.of(System.getProperty("user.home"), ".rechenwerk", "world4you")
private val ip: Path = folder.resolve("ip")
private val ids: Path = folder.resolve("ids")
private val log: Path = folder.resolve("log")

private fun Path.write(str: String) {
    Files.writeString(this, str, StandardOpenOption.APPEND)
}
private fun Path.overwrite(str: String) {
    Files.writeString(this, str, StandardOpenOption.APPEND)
}

fun main(args: Array<String>) {
    init()
    updateIp(args[0], args[1])
}

private fun init() {
    //initialize chromium
    System.setProperty("webdriver.gecko.driver", folder.resolve("geckodriver").toString())
    options.setHeadless(true)

    //create directories and files
    if(Files.notExists(folder)) {
        Files.createDirectories(folder)
    }
    if(Files.notExists(ip)) {
        Files.createFile(ip)
    }
    if(Files.notExists(ids)) {
        Files.createFile(ids)
    }
    if(Files.notExists(log)) {
        Files.createFile(log)
    }
}

private fun updateIp(userName: String, password: String) {
    val currentIP = URL("https://checkip.amazonaws.com/").readText()

    if(Files.readString(ip) == currentIP) {
        log.write(".")
        return
    }

    // Login.
    log.write("\nInconsistent IP at: ${Date()}!\n")
    val driver: WebDriver = FirefoxDriver(options)
    driver.get("https://my.world4you.com/de")
    driver.findElement(ByXPath("/html/body/div[1]/div[2]/div[2]/div[2]/div/div/div/div[1]/form/div[1]/div[1]/div/div[2]/input")).sendKeys(userName)
    driver.findElement(ByXPath("/html/body/div[1]/div[2]/div[2]/div[2]/div/div/div/div[1]/form/div[1]/div[2]/div/div[2]/div/input")).sendKeys(password)
    driver.findElement(ByXPath("/html/body/div[1]/div[2]/div[2]/div[2]/div/div/div/div[1]/form/div[4]/button")).click()
    Thread.sleep(3000)

    try {
        driver.findElement(ByXPath("/html/body/div[1]/div[2]/div[2]/div[2]/div/div/div/div[1]/form/div[4]/button"))
        log.write("  Could not log in.\n")
        driver.close()
        return
    } catch (_: Exception) { }

    for (id in Files.readAllLines(ids)) {

        log.write("  Checking $id for inconsistent IP-Addresses.\n")
        driver.get("https://my.world4you.com/de/${id}/dns")
        Thread.sleep(1000)

        val rows = driver.findElements(ByXPath("/html/body/div[1]/div[4]/div[3]/div[4]/div[10]/div/div/div/div[2]/div/div[2]/div/table/tbody/tr"))
        log.write("  Found ${rows.size} entries.\n")

        for (i in 1 .. rows.size) {

            val col = driver
                .findElements(ByXPath("/html/body/div[1]/div[4]/div[3]/div[4]/div[10]/div/div/div/div[2]/div/div[2]/div/table/tbody/tr${
                    if(rows.size != 1) "[$i]" // Little hack, because tr is messed up in Selenium.
                    else ""                   
                }/td"))

            val name = col[0]
            val type = col[1]
            val value = col[2]
            val editButton = col[3].findElement(ByXPath("./div[1]/div[1]/a"))

            if (type.text == "A") {
                log.write("    ${name.text} is an A-entry with IP-Address: ${value.text}.\n")
                editButton.click()
                Thread.sleep(1000)

                val entry = driver.findElement(ByXPath("/html/body/div[1]/div[4]/div[3]/div[3]/div/div[2]/div/div/div/div/form/div[3]/div[1]/div/div/input"))
                entry.clear()
                entry.sendKeys(currentIP)
            } else {
                log.write("    ${name.text} is an not an A-entry. Skipping entry.\n")
            }
        }
    }
    ip.overwrite(currentIP)
    driver.close()
    log.write("New IP: $currentIP")
}
