package topinambur


val String.needsBody
    get() = this == "POST" || this == "PUT" || this == "DELETE"