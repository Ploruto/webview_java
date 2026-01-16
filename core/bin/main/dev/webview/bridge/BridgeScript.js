(function() {
    if (window.Bridge) return;

    const __bridgeInternal = window.__bridgeInternal;
    delete window.__bridgeInternal; // Hide internal function

    let objectRegistry = {};

    const Bridge = {
        __internal: {
            sendMessageToJava(type, data) {
                return __bridgeInternal(type, data);
            },

            defineObject(path, id) {
                const parts = path.split('.');
                const propertyName = parts.pop();

                let proxy;

                const object = {
                    __internal: {
                        id: id,

                        defineFunction(name) {
                            Object.defineProperty(object, name, {
                                value: function() {
                                    return Bridge.__internal.invoke(
                                        id,
                                        name,
                                        Array.from(arguments)
                                    );
                                }
                            });
                        },

                        defineProperty(name) {
                            Object.defineProperty(object, name, {
                                value: null,
                                writable: true,
                                configurable: true
                            });
                        }
                    }
                };

                const handler = {
                    get(obj, property) {
                        if (typeof obj[property] !== 'undefined' && obj[property] !== null) {
                            return obj[property];
                        }
                        return Bridge.__internal.get(id, property);
                    },
                    set(obj, property, value) {
                        Bridge.__internal.set(id, property, value);
                        return value;
                    }
                };

                Object.freeze(object.__internal);
                proxy = new Proxy(object, handler);

                // Resolve the root object
                let root = window;
                for (const part of parts) {
                    root = root[part];
                }

                Object.defineProperty(root, propertyName, {
                    value: proxy,
                    writable: true,
                    configurable: true
                });

                objectRegistry[id] = proxy;
            },

            get(id, property) {
                return Bridge.__internal.sendMessageToJava('GET', { id, property });
            },

            set(id, property, newValue) {
                return Bridge.__internal.sendMessageToJava('SET', {
                    id,
                    property,
                    newValue
                });
            },

            invoke(id, func, arguments) {
                return Bridge.__internal.sendMessageToJava('INVOKE', {
                    id,
                    function: func,
                    arguments
                });
            }
        }
    };

    Object.freeze(Bridge);
    Object.freeze(Bridge.__internal);
    Object.defineProperty(window, 'Bridge', {
        value: Bridge,
        writable: false,
        configurable: false
    });

    console.log('[Bridge] Initialized');
})();
